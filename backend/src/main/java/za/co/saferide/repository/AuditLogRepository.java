package za.co.saferide.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only access to the `audit_log` table.
 * Joins users to show who did what.
 */
@Repository
public class AuditLogRepository {

    private final JdbcTemplate jdbc;

    public AuditLogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Map<String, Object>> MAPPER = (rs, rowNum) -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("id"));

        long userId = rs.getLong("user_id");
        m.put("userId", rs.wasNull() ? null : userId);

        m.put("action", rs.getString("action"));
        m.put("details", rs.getString("details"));

        Timestamp created = rs.getTimestamp("created_at");
        m.put("createdAt", created == null ? null : created.toLocalDateTime().toString());

        try { m.put("userName", rs.getString("user_name")); } catch (Exception ignored) {}
        try { m.put("userRole", rs.getString("user_role")); } catch (Exception ignored) {}

        return m;
    };

    private static final String BASE_SELECT = """
            SELECT a.*,
                   u.full_name AS user_name,
                   u.role      AS user_role
            FROM audit_log a
            LEFT JOIN users u ON u.id = a.user_id
            """;

    public List<Map<String, Object>> findAll() {
        return jdbc.query(BASE_SELECT + " ORDER BY a.created_at DESC, a.id DESC LIMIT 200", MAPPER);
    }

    /** Distinct action types for filter dropdown. */
    public List<String> distinctActions() {
        return jdbc.queryForList(
                "SELECT DISTINCT action FROM audit_log ORDER BY action",
                String.class
        );
    }

    public int count() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class);
        return n == null ? 0 : n;
    }
}
