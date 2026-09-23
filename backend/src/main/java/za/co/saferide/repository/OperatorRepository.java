package za.co.saferide.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import za.co.saferide.model.Operator;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the `operators` table.
 */
@Repository
public class OperatorRepository {

    private final JdbcTemplate jdbc;

    public OperatorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Operator> MAPPER = (rs, rowNum) -> {
        Operator o = new Operator();
        o.setId(rs.getLong("id"));

        long userId = rs.getLong("user_id");
        o.setUserId(rs.wasNull() ? null : userId);

        o.setCompanyName(rs.getString("company_name"));

        long schoolId = rs.getLong("school_id");
        o.setSchoolId(rs.wasNull() ? null : schoolId);

        Date prdp = rs.getDate("prdp_expiry");
        if (prdp != null) o.setPrdpExpiry(prdp.toLocalDate());

        Date rw = rs.getDate("roadworthy_expiry");
        if (rw != null) o.setRoadworthyExpiry(rw.toLocalDate());

        Date reg = rs.getDate("registration_expiry");
        if (reg != null) o.setRegistrationExpiry(reg.toLocalDate());

        o.setActive(rs.getBoolean("active"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) o.setCreatedAt(created.toLocalDateTime());

        try { o.setContactName(rs.getString("contact_name")); } catch (Exception ignored) {}
        try { o.setContactEmail(rs.getString("contact_email")); } catch (Exception ignored) {}
        try { o.setContactPhone(rs.getString("contact_phone")); } catch (Exception ignored) {}
        try { o.setSchoolName(rs.getString("school_name")); } catch (Exception ignored) {}

        return o;
    };

    private static final String BASE_SELECT = """
            SELECT o.*,
                   u.full_name AS contact_name,
                   u.email     AS contact_email,
                   u.phone     AS contact_phone,
                   s.name      AS school_name
            FROM operators o
            LEFT JOIN users   u ON u.id = o.user_id
            LEFT JOIN schools s ON s.id = o.school_id
            """;

    /** All operators, optionally filtered by school. */
    public List<Operator> findAll(Long schoolId) {
        if (schoolId == null) {
            return jdbc.query(BASE_SELECT + " WHERE o.active = 1 ORDER BY o.company_name", MAPPER);
        }
        return jdbc.query(
                BASE_SELECT + " WHERE o.active = 1 AND o.school_id = ? ORDER BY o.company_name",
                MAPPER, schoolId
        );
    }

    public Optional<Operator> findById(Long id) {
        List<Operator> rows = jdbc.query(BASE_SELECT + " WHERE o.id = ?", MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** Find the operator row belonging to a specific user (used by operator dashboard). */
    public Optional<Operator> findByUserId(Long userId) {
        List<Operator> rows = jdbc.query(BASE_SELECT + " WHERE o.user_id = ?", MAPPER, userId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Long insert(Operator o) {
        String sql = """
                INSERT INTO operators
                  (user_id, company_name, school_id,
                   prdp_expiry, roadworthy_expiry, registration_expiry, active)
                VALUES (?, ?, ?, ?, ?, ?, 1)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, o.getUserId());
            ps.setString(2, o.getCompanyName());
            ps.setLong(3, o.getSchoolId());

            if (o.getPrdpExpiry() == null) ps.setNull(4, java.sql.Types.DATE);
            else ps.setDate(4, Date.valueOf(o.getPrdpExpiry()));

            if (o.getRoadworthyExpiry() == null) ps.setNull(5, java.sql.Types.DATE);
            else ps.setDate(5, Date.valueOf(o.getRoadworthyExpiry()));

            if (o.getRegistrationExpiry() == null) ps.setNull(6, java.sql.Types.DATE);
            else ps.setDate(6, Date.valueOf(o.getRegistrationExpiry()));

            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int updateExpiry(Long id, String field, java.time.LocalDate newDate) {
        String column;
        switch (field) {
            case "prdp":         column = "prdp_expiry"; break;
            case "roadworthy":   column = "roadworthy_expiry"; break;
            case "registration": column = "registration_expiry"; break;
            default: throw new IllegalArgumentException("Unknown compliance field: " + field);
        }
        return jdbc.update(
                "UPDATE operators SET " + column + " = ? WHERE id = ?",
                Date.valueOf(newDate), id
        );
    }

    public int count() {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM operators WHERE active = 1", Integer.class);
        return n == null ? 0 : n;
    }
}
