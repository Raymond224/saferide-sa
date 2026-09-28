package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.saferide.model.Trip;
import za.co.saferide.repository.TripRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Trip endpoints:
 *   GET    /api/trips                → list (optional ?status=active|on-route|...)
 *   GET    /api/trips/:id            → single trip
 *   POST   /api/trips                → create (operator)
 *   PUT    /api/trips/:id/status     → update status (operator/admin)
 */
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripRepository tripRepository;

    public TripController(TripRepository tripRepository) {
        this.tripRepository = tripRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String status,
                                   HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        List<Trip> trips;
        if ("active".equalsIgnoreCase(status)) {
            trips = tripRepository.findActive();
        } else {
            trips = tripRepository.findAll(status);
        }
        return ResponseEntity.ok(trips);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        Optional<Trip> found = tripRepository.findById(id);
        if (found.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Trip not found");
        }
        return ResponseEntity.ok(found.get());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateTripRequest body, HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"operator".equals(role) && !"admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Only operators and admins can create trips");
        }
        if (body == null || body.operatorId == null || body.vehicleId == null) {
            return error(HttpStatus.BAD_REQUEST, "operatorId and vehicleId are required");
        }

        Trip t = new Trip();
        t.setOperatorId(body.operatorId);
        t.setVehicleId(body.vehicleId);
        t.setRouteId(body.routeId);
        t.setRouteName(body.routeName);
        t.setDepartureTime(body.departureTime == null ? LocalDateTime.now() : body.departureTime);
        t.setStatus(Trip.Status.SCHEDULED);
        t.setCurrentLat(body.currentLat);
        t.setCurrentLng(body.currentLng);

        Long id = tripRepository.insert(t);
        return ResponseEntity.ok(Map.of("id", id, "ok", true));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id,
                                           @RequestBody Map<String, String> body,
                                           HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"operator".equals(role) && !"admin".equals(role) && !"system-admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Not allowed");
        }
        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return error(HttpStatus.BAD_REQUEST, "status is required");
        }
        try {
            Trip.Status.fromDb(newStatus);
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, "Invalid status: " + newStatus);
        }
        int updated = tripRepository.updateStatus(id, newStatus);
        if (updated == 0) {
            return error(HttpStatus.NOT_FOUND, "Trip not found");
        }
        return ResponseEntity.ok(Map.of("ok", true, "id", id, "status", newStatus));
    }

    // -------- helpers --------

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    /** Request body for creating a trip. */
    public static class CreateTripRequest {
        public Long operatorId;
        public Long vehicleId;
        public Long routeId;
        public String routeName;
        public LocalDateTime departureTime;
        public java.math.BigDecimal currentLat;
        public java.math.BigDecimal currentLng;
    }
}
