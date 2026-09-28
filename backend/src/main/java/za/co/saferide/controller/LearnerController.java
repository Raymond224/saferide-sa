package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.saferide.repository.LearnerRepository;

import java.util.List;
import java.util.Map;

/**
 * Learner endpoints (parent-facing):
 *   GET /api/learners/mine              → the logged-in parent's children + current trip
 *   GET /api/learners/mine?tripId=X     → only children on that trip
 *   GET /api/learners/:id               → a single learner (parent must own it)
 */
@RestController
@RequestMapping("/api/learners")
public class LearnerController {

    private final LearnerRepository learnerRepository;

    public LearnerController(LearnerRepository learnerRepository) {
        this.learnerRepository = learnerRepository;
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(@RequestParam(required = false) Long tripId,
                                   HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        List<Map<String, Object>> learners =
                learnerRepository.findByParentId(((Number) userId).longValue());

        if (tripId != null) {
            learners = learners.stream()
                    .filter(l -> {
                        Object tid = l.get("tripId");
                        return tid != null && tid.toString().equals(tripId.toString());
                    })
                    .toList();
        }
        return ResponseEntity.ok(learners);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        List<Map<String, Object>> found = learnerRepository.findById(id);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Learner not found");
        }
        Map<String, Object> learner = found.get(0);
        String role = (String) session.getAttribute("role");
        if ("parent".equals(role)) {
            Object parentId = learner.get("parentId");
            if (parentId == null ||
                !parentId.toString().equals(userId.toString())) {
                return error(HttpStatus.FORBIDDEN, "Not your learner");
            }
        }
        return ResponseEntity.ok(learner);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
