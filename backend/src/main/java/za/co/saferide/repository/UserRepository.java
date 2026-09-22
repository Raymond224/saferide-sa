package za.co.saferide.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import za.co.saferide.model.User;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the `users` table.
 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<User> MAPPER = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(User.Role.fromDb(rs.getString("role")));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));

        long schoolId = rs.getLong("school_id");
        u.setSchoolId(rs.wasNull() ? null : schoolId);

        u.setActive(rs.getBoolean("active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) u.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) u.setUpdatedAt(updatedAt.toLocalDateTime());

        Timestamp deletedAt = rs.getTimestamp("deleted_at");
        if (deletedAt != null) u.setDeletedAt(deletedAt.toLocalDateTime());

        return u;
    };

    /** Find a user by username (active users only). */
    public Optional<User> findByUsername(String username) {
        List<User> users = jdbc.query(
                "SELECT * FROM users WHERE username = ? AND active = 1 AND deleted_at IS NULL",
                MAPPER, username
        );
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    /** Find a user by id. */
    public Optional<User> findById(Long id) {
        List<User> users = jdbc.query(
                "SELECT * FROM users WHERE id = ? AND deleted_at IS NULL",
                MAPPER, id
        );
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    /** List all users, optionally filtered by role. */
    public List<User> findAll(String roleFilter) {
        if (roleFilter == null || roleFilter.isBlank()) {
            return jdbc.query(
                    "SELECT * FROM users WHERE deleted_at IS NULL ORDER BY id",
                    MAPPER
            );
        }
        return jdbc.query(
                "SELECT * FROM users WHERE role = ? AND deleted_at IS NULL ORDER BY id",
                MAPPER, roleFilter
        );
    }

    /** Insert a new user and return the generated id. */
    public Long insert(User u) {
        String sql = """
                INSERT INTO users
                  (username, password_hash, role, full_name, email, phone, school_id, active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getRoleString());
            ps.setString(4, u.getFullName());
            ps.setString(5, u.getEmail());
            ps.setString(6, u.getPhone());
            if (u.getSchoolId() == null) ps.setNull(7, java.sql.Types.BIGINT);
            else ps.setLong(7, u.getSchoolId());
            ps.setBoolean(8, u.isActive());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    /** Soft-delete a user (keeps audit history). */
    public int softDelete(Long id) {
        return jdbc.update(
                "UPDATE users SET deleted_at = CURRENT_TIMESTAMP, active = 0 WHERE id = ?",
                id
        );
    }

    /** Count users (useful for health checks / dashboards). */
    public int count() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL",
                Integer.class
        );
        return n == null ? 0 : n;
    }
}
