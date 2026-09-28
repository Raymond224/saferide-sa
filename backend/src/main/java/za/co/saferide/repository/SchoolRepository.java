package za.co.saferide.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JDBC access to the `schools` table.
 * Returns Maps (not entities) since schools are simple.
 */
@Repository
public class SchoolRepository {

    private final JdbcTemplate jdbc;

    public SchoolRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Map<String, Object>> MAPPER = (rs, rowNum) -> {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rs.getLong("id"));
        m.put("name", rs.getString("name"));
        m.put("address", rs.getString("address"));
        m.put("phone", rs.getString("phone"));
        m.put("email", rs.getString("email"));

        Timestamp created = rs.getTimestamp("created_at");
        m.put("createdAt", created == null ? null : created.toLocalDateTime().toString());

        // Optional joined count of users
        try { m.put("userCount", rs.getInt("user_count")); } catch (Exception ignored) {}

        return m;
    };

    public List<Map<String, Object>> findAll() {
        String sql = """
                SELECT s.*, COUNT(u.id) AS user_count
                FROM schools s
                LEFT JOIN users u ON u.school_id = s.id AND u.deleted_at IS NULL
                GROUP BY s.id
                ORDER BY s.name
                """;
        return jdbc.query(sql, MAPPER);
    }

    public Optional<Map<String, Object>> findById(Long id) {
        String sql = "SELECT * FROM schools WHERE id = ?";
        List<Map<String, Object>> rows = jdbc.query(sql, MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Long insert(String name, String address, String phone, String email) {
        String sql = "INSERT INTO schools (name, address, phone, email) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, phone);
            ps.setString(4, email);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int count() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM schools", Integer.class);
        return n == null ? 0 : n;
    }
}
