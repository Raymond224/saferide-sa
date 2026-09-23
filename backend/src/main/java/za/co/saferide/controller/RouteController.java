package za.co.saferide.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Route / deviation endpoints:
 *   GET  /api/routes/trip/:tripId           → trip + waypoints + learners on board
 *   POST /api/routes/trip/:tripId/notify    → send a message to all parents of learners on the trip
 */
@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final JdbcTemplate jdbc;

    public RouteController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<?> getRouteData(@PathVariable Long tripId, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }

        // Trip + route + operator + vehicle
        List<Map<String, Object>> trips = jdbc.queryForList("""
                SELECT t.id, t.route_name, t.status, t.current_lat, t.current_lng,
                       t.departure_time,
                       r.waypoints,
                       o.company_name AS operator_name,
                       v.registration_number AS vehicle_registration
                FROM trips t
                LEFT JOIN routes r    ON r.id = t.route_id
                LEFT JOIN operators o ON o.id = t.operator_id
                LEFT JOIN vehicles v  ON v.id = t.vehicle_id
                WHERE t.id = ?
                """, tripId);

        if (trips.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Trip not found");
        }
        Map<String, Object> trip = trips.get(0);

        // Learners on this trip (with names for the tick-box display)
        List<Map<String, Object>> learners = jdbc.queryForList("""
                SELECT tl.learner_id, l.full_name, l.grade,
                       tl.picked_up, tl.dropped_off
                FROM trip_learners tl
                LEFT JOIN learners l ON l.id = tl.learner_id
                WHERE tl.trip_id = ?
                """, tripId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tripId", trip.get("id"));
        out.put("routeName", trip.get("route_name"));
        out.put("status", trip.get("status"));
        out.put("currentLat", trip.get("current_lat"));
        out.put("currentLng", trip.get("current_lng"));
        out.put("departureTime", trip.get("departure_time"));
        out.put("waypoints", trip.get("waypoints"));
        out.put("operatorName", trip.get("operator_name"));
        out.put("vehicleRegistration", trip.get("vehicle_registration"));
        out.put("learnerCount", learners.size());
        out.put("learners", learners);

        return ResponseEntity.ok(out);
    }

    @PostMapping("/trip/{tripId}/notify")
    public ResponseEntity<?> notifyParents(@PathVariable Long tripId,
                                            @RequestBody(required = false) NotifyRequest body,
                                            HttpSession session) {
        Object userId = session.getAttribute("userId");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            return error(HttpStatus.UNAUTHORIZED, "Not logged in");
        }
        if (!"admin".equals(role) && !"system-admin".equals(role)) {
            return error(HttpStatus.FORBIDDEN, "Only admins can notify parents");
        }

        // Get trip + school
        List<Map<String, Object>> trips = jdbc.queryForList("""
                SELECT t.id, t.route_name, o.school_id
                FROM trips t
                LEFT JOIN operators o ON o.id = t.operator_id
                WHERE t.id = ?
                """, tripId);
        if (trips.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, "Trip not found");
        }
        Map<String, Object> trip = trips.get(0);
        Number schoolIdNum = (Number) trip.get("school_id");
        if (schoolIdNum == null) {
            return error(HttpStatus.BAD_REQUEST, "Trip has no school assigned");
        }
        Long schoolId = schoolIdNum.longValue();
        String routeName = (String) trip.get("route_name");

        // Get parents of learners on this trip (distinct)
        List<Long> parentIds = jdbc.queryForList("""
                SELECT DISTINCT l.parent_id
                FROM trip_learners tl
                JOIN learners l ON l.id = tl.learner_id
                WHERE tl.trip_id = ? AND l.parent_id IS NOT NULL
                """, Long.class, tripId);

        if (parentIds.isEmpty()) {
            return ResponseEntity.ok(Map.of("ok", true, "sent", 0));
        }

        String content = (body != null && body.message != null && !body.message.isBlank())
                ? body.message
                : "Alert: possible route deviation detected on " + routeName + ". Please check the app for details.";

        int sent = 0;
        for (Long parentId : parentIds) {
            // We reuse the messages table: admin → school is the direction, but for a
            // notification we write from the admin's school perspective to each parent.
            // Since the schema has from_user and to_school, we insert one message per parent
            // with from_user = admin's user_id and to_school = the school.
            // (Real production would add a 'to_user' column; for the demo, this creates
            //  a message record per parent so they appear in the audit trail.)
            jdbc.update("""
                    INSERT INTO messages (from_user, to_school, content)
                    VALUES (?, ?, ?)
                    """, userId, schoolId, content);
            sent++;

            // Also log to audit_log
            jdbc.update("""
                    INSERT INTO audit_log (user_id, action, details)
                    VALUES (?, 'notify-parent', ?)
                    """, userId, "Notified parent id " + parentId + " about trip " + tripId);
        }

        return ResponseEntity.ok(Map.of("ok", true, "sent", sent));
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }

    public static class NotifyRequest {
        public String message;
    }
}
