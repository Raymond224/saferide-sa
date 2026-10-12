package za.co.saferide.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import za.co.saferide.model.Incident;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the `incidents` table.
 * Joins users (reporter) and trips (route name) for display.
 */
@Repository
public class IncidentRepository {

    private final JdbcTemplate jdbc;

    public IncidentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Incident> MAPPER = (rs, rowNum) -> {
        Incident i = new Incident();
        i.setId(rs.getLong("id"));

        long reportedBy = rs.getLong("reported_by");
        i.setReportedBy(rs.wasNull() ? null : reportedBy);

        long tripId = rs.getLong("trip_id");
        i.setTripId(rs.wasNull() ? null : tripId);

        i.setType(rs.getString("type"));
        i.setDescription(rs.getString("description"));
        i.setStatus(Incident.Status.fromDb(rs.getString("status")));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) i.setCreatedAt(created.toLocalDateTime());

        Timestamp resolved = rs.getTimestamp("resolved_at");
        if (resolved != null) i.setResolvedAt(resolved.toLocalDateTime());

        // Joined display fields (may be null depending on query)
        try { i.setReporterName(rs.getString("reporter_name")); } catch (Exception ignored) {}
        try { i.setReporterRole(rs.getString("reporter_role")); } catch (Exception ignored) {}
        try { i.setTripRouteName(rs.getString("trip_route_name")); } catch (Exception ignored) {}

        return i;
    };

    private static final String BASE_SELECT = """
            SELECT i.*,
                   u.full_name AS reporter_name,
                   u.role      AS reporter_role,
                   t.route_name AS trip_route_name
            FROM incidents i
            LEFT JOIN users u ON u.id = i.reported_by
            LEFT JOIN trips t ON t.id = i.trip_id
            """;

    /** All incidents, optionally filtered by status. Newest first. */
    public List<Incident> findAll(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return jdbc.query(BASE_SELECT + " ORDER BY i.created_at DESC, i.id DESC", MAPPER);
        }
        return jdbc.query(
                BASE_SELECT + " WHERE i.status = ? ORDER BY i.created_at DESC, i.id DESC",
                MAPPER, statusFilter
        );
    }

    public Optional<Incident> findById(Long id) {
        List<Incident> rows = jdbc.query(BASE_SELECT + " WHERE i.id = ?", MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Incidents reported by a specific user (for parent dashboard). */
    public List<Incident> findByReporter(Long userId) {
        return jdbc.query(
                BASE_SELECT + " WHERE i.reported_by = ? ORDER BY i.created_at DESC",
                MAPPER, userId
        );
    }

    public Long insert(Incident i) {
        String sql = """
                INSERT INTO incidents
                  (reported_by, trip_id, type, description, status)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, i.getReportedBy());

            if (i.getTripId() == null) ps.setNull(2, java.sql.Types.BIGINT);
            else ps.setLong(2, i.getTripId());

            ps.setString(3, i.getType());
            ps.setString(4, i.getDescription());
            ps.setString(5, i.getStatusString());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int updateStatus(Long id, String status) {
        if ("resolved".equals(status)) {
            return jdbc.update(
                    "UPDATE incidents SET status = ?, resolved_at = CURRENT_TIMESTAMP WHERE id = ?",
                    status, id
            );
        }
        return jdbc.update("UPDATE incidents SET status = ? WHERE id = ?", status, id);
    }

    public int countOpen() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM incidents WHERE status = 'open'",
                Integer.class
        );
        return n == null ? 0 : n;
    }

    public int count() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM incidents", Integer.class);
        return n == null ? 0 : n;
    }
}
