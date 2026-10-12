package za.co.saferide.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import za.co.saferide.model.Message;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the `messages` table.
 */
@Repository
public class MessageRepository {

    private final JdbcTemplate jdbc;

    public MessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Message> MAPPER = (rs, rowNum) -> {
        Message m = new Message();
        m.setId(rs.getLong("id"));

        long fromUser = rs.getLong("from_user");
        m.setFromUser(rs.wasNull() ? null : fromUser);

        long toSchool = rs.getLong("to_school");
        m.setToSchool(rs.wasNull() ? null : toSchool);

        m.setContent(rs.getString("content"));

        long replyTo = rs.getLong("reply_to");
        m.setReplyTo(rs.wasNull() ? null : replyTo);

        Timestamp readAt = rs.getTimestamp("read_at");
        if (readAt != null) m.setReadAt(readAt.toLocalDateTime());

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) m.setCreatedAt(created.toLocalDateTime());

        try { m.setFromUserName(rs.getString("from_user_name")); } catch (Exception ignored) {}
        try { m.setFromUserRole(rs.getString("from_user_role")); } catch (Exception ignored) {}
        try { m.setSchoolName(rs.getString("school_name")); } catch (Exception ignored) {}

        return m;
    };

    private static final String BASE_SELECT = """
            SELECT m.*,
                   u.full_name AS from_user_name,
                   u.role      AS from_user_role,
                   s.name      AS school_name
            FROM messages m
            LEFT JOIN users   u ON u.id = m.from_user
            LEFT JOIN schools s ON s.id = m.to_school
            """;

    /** All messages for a school (admin view). Newest first. */
    public List<Message> findBySchool(Long schoolId) {
        if (schoolId == null) {
            return jdbc.query(BASE_SELECT + " ORDER BY m.created_at DESC", MAPPER);
        }
        return jdbc.query(
                BASE_SELECT + " WHERE m.to_school = ? ORDER BY m.created_at DESC",
                MAPPER, schoolId
        );
    }

    /** All messages (system admin view). Newest first. */
    public List<Message> findAll() {
        return jdbc.query(BASE_SELECT + " ORDER BY m.created_at DESC", MAPPER);
    }

    /** Messages sent by a specific parent (their outbox). */
    public List<Message> findBySender(Long userId) {
        return jdbc.query(
                BASE_SELECT + " WHERE m.from_user = ? ORDER BY m.created_at DESC",
                MAPPER, userId
        );
    }

    public Optional<Message> findById(Long id) {
        List<Message> rows = jdbc.query(BASE_SELECT + " WHERE m.id = ?", MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Long insert(Message m) {
        String sql = """
                INSERT INTO messages (from_user, to_school, content, reply_to)
                VALUES (?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, m.getFromUser());
            ps.setLong(2, m.getToSchool());
            ps.setString(3, m.getContent());

            if (m.getReplyTo() == null) ps.setNull(4, java.sql.Types.BIGINT);
            else ps.setLong(4, m.getReplyTo());

            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int markRead(Long id) {
        return jdbc.update(
                "UPDATE messages SET read_at = CURRENT_TIMESTAMP WHERE id = ? AND read_at IS NULL",
                id
        );
    }

    public int countUnread(Long schoolId) {
        Integer n;
        if (schoolId == null) {
            n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM messages WHERE read_at IS NULL", Integer.class);
        } else {
            n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM messages WHERE to_school = ? AND read_at IS NULL",
                    Integer.class, schoolId);
        }
        return n == null ? 0 : n;
    }

    public int countAll(Long schoolId) {
        Integer n;
        if (schoolId == null) {
            n = jdbc.queryForObject("SELECT COUNT(*) FROM messages", Integer.class);
        } else {
            n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM messages WHERE to_school = ?",
                    Integer.class, schoolId);
        }
        return n == null ? 0 : n;
    }
}
