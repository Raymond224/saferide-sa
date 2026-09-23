package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.saferide.model.Incident;
import za.co.saferide.repository.IncidentRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Incident endpoints:
 *   GET    /api/incidents                    → list (optional ?status=open|investigating|resolved)
 *   GET    /api/incidents/:id                → single incident
 *   GET    /api/incidents/mine               → incidents reported by current user
 *   POST   /api/incidents                    → create (parent or operator)
 *   PUT    /api/incidents/:id                → update status (admin only)
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;

    public IncidentController(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String status,
                                   HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        // Admins and system-admins see all; parents see only their own
        List<Incident> incidents;
        if ("admin".equals(role) || "system-admin".equals(role)) {
            incidents = incidentRepository.findAll(status);
        } else if ("parent".equals(role)) {
            incidents = incidentRepository.findByReporter(((Number) userId).longValue());
        } else {
            // Operators see all too (simplified for demo)
            incidents = incidentRepository.findAll(status);
        }
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        return ResponseEntity.ok(
                incidentRepository.findByReporter(((Number) userId).longValue())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Optional<Incident> found = incidentRepository.findById(id);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Incident not found");
        }
        return ResponseEntity.ok(found.get());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateIncidentRequest body, HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"parent".equals(role) && !"operator".equals(role) && !"admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Not allowed to report incidents");
        }
        if (body == null || body.type == null || body.type.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "type is required");
        }

        Incident i = new Incident();
        i.setReportedBy(((Number) userId).longValue());
        i.setTripId(body.tripId);
        i.setType(body.type);
        i.setDescription(body.description);
        i.setStatus(Incident.Status.OPEN);

        Long id = incidentRepository.insert(i);
        return ResponseEntity.ok(Map.of("id", id, "ok", true));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @RequestBody Map<String, String> body,
                                     HttpSession session) {
        String role = (String) session.getAttribute("role");
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"admin".equals(role) && !"system-admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Only admins can update incident status");
        }
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "status is required");
        }
        try {
            Incident.Status.fromDb(newStatus);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "Invalid status: " + newStatus);
        }
        int updated = incidentRepository.updateStatus(id, newStatus);
        if (updated == 0) {
            return error(HttpStatus.NOT_FOUND, "Incident not found");
        }
        return ResponseEntity.ok(Map.of("ok", true, "id", id, "status", newStatus));
    }

    // -------- helpers --------

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    /** Request body for creating an incident. */
    public static class CreateIncidentRequest {
        public Long tripId;
        public String type;
        public String description;
    }
}
