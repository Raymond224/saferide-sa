package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.saferide.repository.AuditLogRepository;

import java.util.Map;

/**
 * Audit log endpoints:
 *   GET /api/audit         → all entries (newest first)
 *   GET /api/audit/actions → distinct action types
 */
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"system-admin".equals(role) && !"admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Only admins can view the audit log");
        }
        return ResponseEntity.ok(auditLogRepository.findAll());
    }

    @GetMapping("/actions")
    public ResponseEntity<?> actions(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return ResponseEntity.ok(auditLogRepository.distinctActions());
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
