package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.saferide.model.Learner;
import za.co.saferide.repository.LearnerRepository;

import java.util.List;
import java.util.Map;

/**
 * Learner endpoints (parent-facing):
 *   GET /api/learners/mine → the current parent's registered children,
 *                             each with their current trip status.
 */
@RestController
@RequestMapping("/api/learners")
public class LearnerController {

    private final LearnerRepository learnerRepository;

    public LearnerController(LearnerRepository learnerRepository) {
        this.learnerRepository = learnerRepository;
    }

    /**
     * The current parent's children. Optionally pass ?tripId= to see just
     * the children (and their pick-up/drop-off status) on one specific trip.
     */
    @GetMapping("/mine")
    public ResponseEntity<?> mine(@RequestParam(required = false) Long tripId, HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"parent".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Only parents can use this endpoint");
        }
        Long parentId = ((Number) userId).longValue();
        List<Learner> learners = (tripId == null)
                ? learnerRepository.findByParent(parentId)
                : learnerRepository.findByParentAndTrip(parentId, tripId);
        return ResponseEntity.ok(learners);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
