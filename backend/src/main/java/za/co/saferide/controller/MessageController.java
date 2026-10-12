package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.saferide.model.Message;
import za.co.saferide.repository.MessageRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Message endpoints:
 *   GET    /api/messages           → list (admin sees school inbox; parent sees their outbox)
 *   GET    /api/messages/:id       → one message (auto-marks read for admin)
 *   POST   /api/messages           → send new message (parent → school)
 *   POST   /api/messages/:id/reply → reply to a message (school → parent)
 *   GET    /api/messages/summary   → unread count for the dashboard
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageRepository messageRepository;

    public MessageController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        List<Message> messages;
        if ("parent".equals(role)) {
            // Parents see their own outbox
            messages = messageRepository.findBySender(((Number) userId).longValue());
        } else if ("system-admin".equals(role)) {
            // System admins see everything
            messages = messageRepository.findAll();
        } else {
            // Admins and operators see their school's inbox
            Long schoolId = sessionSchoolId(session);
            messages = messageRepository.findBySchool(schoolId);
        }
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary(HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        Long schoolId = sessionSchoolId(session);
        int unread;
        int total;

        if ("parent".equals(role)) {
            List<Message> mine = messageRepository.findBySender(((Number) userId).longValue());
            total = mine.size();
            unread = (int) mine.stream().filter(m -> m.getReadAt() == null).count();
        } else if ("system-admin".equals(role)) {
            total = messageRepository.countAll(null);
            unread = messageRepository.countUnread(null);
        } else {
            total = messageRepository.countAll(schoolId);
            unread = messageRepository.countUnread(schoolId);
        }

        return ResponseEntity.ok(Map.of("total", total, "unread", unread));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Optional<Message> found = messageRepository.findById(id);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Message not found");
        }
        // Auto-mark as read if the viewer isn't the sender
        if (!"parent".equals(role)) {
            messageRepository.markRead(id);
        }
        return ResponseEntity.ok(found.get());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateMessageRequest body, HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (body == null || body.content == null || body.content.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "content is required");
        }
        if (body.toSchool == null) {
            return error(HttpStatus.BAD_REQUEST, "toSchool is required");
        }
        // Only parents (and admins for testing) can send
        if (!"parent".equals(role) && !"admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Only parents can send messages");
        }

        Message m = new Message();
        m.setFromUser(((Number) userId).longValue());
        m.setToSchool(body.toSchool);
        m.setContent(body.content);
        m.setReplyTo(body.replyTo);

        Long id = messageRepository.insert(m);
        return ResponseEntity.ok(Map.of("id", id, "ok", true));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<?> reply(@PathVariable Long id,
                                    @RequestBody CreateMessageRequest body,
                                    HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"admin".equals(role) && !"system-admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Only school admins can reply");
        }
        if (body == null || body.content == null || body.content.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "content is required");
        }

        Optional<Message> parent = messageRepository.findById(id);
        if (parent.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Parent message not found");
        }

        Message m = new Message();
        m.setFromUser(((Number) userId).longValue());
        m.setToSchool(parent.get().getToSchool());
        m.setContent(body.content);
        m.setReplyTo(id);

        Long newId = messageRepository.insert(m);
        messageRepository.markRead(id); // mark original as read since admin replied
        return ResponseEntity.ok(Map.of("id", newId, "ok", true));
    }

    // -------- helpers --------

    private Long sessionSchoolId(HttpSession session) {
        Object v = session.getAttribute("schoolId");
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return null; }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    /** Request body for sending/reply. */
    public static class CreateMessageRequest {
        public Long toSchool;
        public String content;
        public Long replyTo;
    }
}
