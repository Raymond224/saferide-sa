package za.co.saferide.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only access to the `learners` table,
 * with their current trip info joined in.
 */
@Repository
public class LearnerRepository {

    private final JdbcTemplate jdbc;

    public LearnerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Map<String, Object>> MAPPER = (rs, rowNum) -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("id"));
        m.put("fullName", rs.getString("full_name"));
        m.put("grade", rs.getString("grade"));
        m.put("parentId", rs.getLong("parent_id"));
        m.put("schoolId", rs.getLong("school_id"));

        try { m.put("schoolName", rs.getString("school_name")); } catch (Exception ignored) {}
        try { m.put("currentTripId", rs.getObject("trip_id")); } catch (Exception ignored) {}
        try { m.put("currentRouteName", rs.getString("trip_route_name")); } catch (Exception ignored) {}
        try { m.put("currentTripStatus", rs.getString("trip_status")); } catch (Exception ignored) {}
        try { m.put("vehicleRegistration", rs.getString("vehicle_registration")); } catch (Exception ignored) {}
        try { m.put("pickedUp", rs.getObject("picked_up")); } catch (Exception ignored) {}
        try { m.put("droppedOff", rs.getObject("dropped_off")); } catch (Exception ignored) {}
        try {
            Timestamp ts = rs.getTimestamp("picked_up_at");
            if (ts != null) m.put("pickedUpAt", ts.toLocalDateTime().toString());
        } catch (Exception ignored) {}

        return m;
    };
    public List<Map<String, Object>> findByParentId(Long parentId) {
    String sql = """
            SELECT l.id, l.full_name, l.grade, l.parent_id, l.school_id,
                   s.name AS school_name,
                   (SELECT t.id FROM trip_learners tl
                      JOIN trips t ON t.id = tl.trip_id
                      WHERE tl.learner_id = l.id
                        AND t.status IN ('on-route','delayed','deviated','scheduled')
                      ORDER BY t.id DESC LIMIT 1) AS trip_id,
                   (SELECT t.route_name FROM trip_learners tl
                      JOIN trips t ON t.id = tl.trip_id
                      WHERE tl.learner_id = l.id
                        AND t.status IN ('on-route','delayed','deviated','scheduled')
                      ORDER BY t.id DESC LIMIT 1) AS trip_route_name,
                   (SELECT t.status FROM trip_learners tl
                      JOIN trips t ON t.id = tl.trip_id
                      WHERE tl.learner_id = l.id
                        AND t.status IN ('on-route','delayed','deviated','scheduled')
                      ORDER BY t.id DESC LIMIT 1) AS trip_status,
                   (SELECT v.registration_number FROM trip_learners tl
                      JOIN trips t ON t.id = tl.trip_id
                      LEFT JOIN vehicles v ON v.id = t.vehicle_id
                      WHERE tl.learner_id = l.id
                        AND t.status IN ('on-route','delayed','deviated','scheduled')
                      ORDER BY t.id DESC LIMIT 1) AS vehicle_registration,
                   (SELECT tl.picked_up FROM trip_learners tl
                      JOIN trips t ON t.id = tl.trip_id
                      WHERE tl.learner_id = l.id
                        AND t.status IN ('on-route','delayed','deviated','scheduled')
                      ORDER BY t.id DESC LIMIT 1) AS picked_up,
                   (SELECT tl.dropped_off FROM trip_learners tl
                      JOIN trips t ON t.id = tl.trip_id
                      WHERE tl.learner_id = l.id
                        AND t.status IN ('on-route','delayed','deviated','scheduled')
                      ORDER BY t.id DESC LIMIT 1) AS dropped_off,
                   (SELECT tl.picked_up_at FROM trip_learners tl
                      JOIN trips t ON t.id = tl.trip_id
                      WHERE tl.learner_id = l.id
                        AND t.status IN ('on-route','delayed','deviated','scheduled')
                      ORDER BY t.id DESC LIMIT 1) AS picked_up_at
            FROM learners l
            LEFT JOIN schools s ON s.id = l.school_id
            WHERE l.parent_id = ?
            ORDER BY l.full_name
            """;

    return jdbc.query(sql, MAPPER, parentId);
}


    public List<Map<String, Object>> findById(Long learnerId) {
        String sql = """
                SELECT l.id, l.full_name, l.grade, l.parent_id, l.school_id,
                       s.name AS school_name
                FROM learners l
                LEFT JOIN schools s ON s.id = l.school_id
                WHERE l.id = ?
                """;
        return jdbc.query(sql, MAPPER, learnerId);
    }

public List<Map<String, Object>> findByParentIdAndTripId(Long parentId, Long tripId) {
    String sql = """
            SELECT l.id, l.full_name, l.grade, l.parent_id, l.school_id,
                   s.name AS school_name,
                   t.id AS trip_id,
                   t.route_name AS trip_route_name,
                   t.status AS trip_status,
                   v.registration_number AS vehicle_registration,
                   tl.picked_up AS picked_up,
                   tl.dropped_off AS dropped_off,
                   tl.picked_up_at AS picked_up_at,
                   tl.dropped_off_at AS dropped_off_at
            FROM learners l
            JOIN trip_learners tl ON tl.learner_id = l.id
            JOIN trips t ON t.id = tl.trip_id
            LEFT JOIN schools s ON s.id = l.school_id
            LEFT JOIN vehicles v ON v.id = t.vehicle_id
            WHERE l.parent_id = ?
              AND t.id = ?
            ORDER BY l.full_name
            """;

    return jdbc.query(sql, MAPPER, parentId, tripId);
}

}
