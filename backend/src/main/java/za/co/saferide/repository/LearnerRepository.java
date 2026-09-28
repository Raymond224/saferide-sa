package za.co.saferide.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import za.co.saferide.model.Learner;

import java.sql.Timestamp;
import java.util.List;

/**
 * JDBC access to the `learners` table.
 * Joins schools, and (for findByParent) the learner's most recent
 * trip_learners/trips row — identified by the highest trip_learners.id,
 * since it is auto-incrementing — so parent screens can show a live status
 * per child without a second round trip.
 */
@Repository
public class LearnerRepository {

    private final JdbcTemplate jdbc;

    public LearnerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Learner> MAPPER = (rs, rowNum) -> {
        Learner l = new Learner();
        l.setId(rs.getLong("id"));
        l.setFullName(rs.getString("full_name"));
        l.setGrade(rs.getString("grade"));

        long parentId = rs.getLong("parent_id");
        l.setParentId(rs.wasNull() ? null : parentId);

        long schoolId = rs.getLong("school_id");
        l.setSchoolId(rs.wasNull() ? null : schoolId);

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) l.setCreatedAt(created.toLocalDateTime());

        try { l.setSchoolName(rs.getString("school_name")); } catch (Exception ignored) {}

        try {
            long tripId = rs.getLong("current_trip_id");
            l.setCurrentTripId(rs.wasNull() ? null : tripId);
        } catch (Exception ignored) {}

        try { l.setCurrentTripStatus(rs.getString("current_trip_status")); } catch (Exception ignored) {}
        try { l.setCurrentRouteName(rs.getString("current_route_name")); } catch (Exception ignored) {}

        try {
            Timestamp dep = rs.getTimestamp("current_departure_time");
            if (dep != null) l.setCurrentDepartureTime(dep.toLocalDateTime());
        } catch (Exception ignored) {}

        try {
            boolean pu = rs.getBoolean("picked_up");
            l.setPickedUp(rs.wasNull() ? null : pu);
        } catch (Exception ignored) {}

        try {
            boolean dof = rs.getBoolean("dropped_off");
            l.setDroppedOff(rs.wasNull() ? null : dof);
        } catch (Exception ignored) {}

        try {
            Timestamp pu = rs.getTimestamp("picked_up_at");
            if (pu != null) l.setPickedUpAt(pu.toLocalDateTime());
        } catch (Exception ignored) {}

        try {
            Timestamp dof = rs.getTimestamp("dropped_off_at");
            if (dof != null) l.setDroppedOffAt(dof.toLocalDateTime());
        } catch (Exception ignored) {}

        return l;
    };

    /**
     * A parent's registered children, each with their most recent trip
     * assignment (if any), for the "My Children" section of the dashboard.
     */
    public List<Learner> findByParent(Long parentId) {
        String sql = """
                SELECT l.*, s.name AS school_name,
                       lt.trip_id AS current_trip_id,
                       t.status AS current_trip_status,
                       t.route_name AS current_route_name,
                       t.departure_time AS current_departure_time,
                       lt.picked_up, lt.dropped_off,
                       lt.picked_up_at, lt.dropped_off_at
                FROM learners l
                JOIN schools s ON s.id = l.school_id
                LEFT JOIN (
                    SELECT tl.* FROM trip_learners tl
                    WHERE tl.id IN (SELECT MAX(id) FROM trip_learners GROUP BY learner_id)
                ) lt ON lt.learner_id = l.id
                LEFT JOIN trips t ON t.id = lt.trip_id
                WHERE l.parent_id = ?
                ORDER BY l.full_name
                """;
        return jdbc.query(sql, MAPPER, parentId);
    }

    /**
     * A parent's children who were on a specific trip, with each child's
     * individual pick-up/drop-off status for that trip.
     */
    public List<Learner> findByParentAndTrip(Long parentId, Long tripId) {
        String sql = """
                SELECT l.*, s.name AS school_name,
                       tl.trip_id AS current_trip_id,
                       tl.picked_up, tl.dropped_off,
                       tl.picked_up_at, tl.dropped_off_at
                FROM learners l
                JOIN schools s ON s.id = l.school_id
                JOIN trip_learners tl ON tl.learner_id = l.id
                WHERE l.parent_id = ? AND tl.trip_id = ?
                ORDER BY l.full_name
                """;
        return jdbc.query(sql, MAPPER, parentId, tripId);
    }

    /** Number of children registered to a parent (dashboard stat card). */
    public int count(Long parentId) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM learners WHERE parent_id = ?",
                Integer.class, parentId
        );
        return n == null ? 0 : n;
    }
}
