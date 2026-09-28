package za.co.saferide.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import za.co.saferide.model.Trip;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * JDBC access to the `trips` table.
 * Includes joins to operators and vehicles for display fields.
 */
@Repository
public class TripRepository {

    private final JdbcTemplate jdbc;

    public TripRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Trip> MAPPER = (rs, rowNum) -> {
        Trip t = new Trip();
        t.setId(rs.getLong("id"));

        long opId = rs.getLong("operator_id");
        t.setOperatorId(rs.wasNull() ? null : opId);

        long vId = rs.getLong("vehicle_id");
        t.setVehicleId(rs.wasNull() ? null : vId);

        long rId = rs.getLong("route_id");
        t.setRouteId(rs.wasNull() ? null : rId);

        t.setRouteName(rs.getString("route_name"));

        Timestamp dep = rs.getTimestamp("departure_time");
        if (dep != null) t.setDepartureTime(dep.toLocalDateTime());

        Timestamp arr = rs.getTimestamp("arrival_time");
        if (arr != null) t.setArrivalTime(arr.toLocalDateTime());

        t.setStatus(Trip.Status.fromDb(rs.getString("status")));

        t.setCurrentLat(rs.getBigDecimal("current_lat"));
        t.setCurrentLng(rs.getBigDecimal("current_lng"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) t.setCreatedAt(created.toLocalDateTime());

        // Optional joined columns (may not exist in every query)
        try { t.setOperatorName(rs.getString("operator_name")); } catch (Exception ignored) {}
        try { t.setVehicleRegistration(rs.getString("vehicle_registration")); } catch (Exception ignored) {}

        return t;
    };

    private static final String BASE_SELECT = """
            SELECT t.*,
                   o.company_name    AS operator_name,
                   v.registration_number AS vehicle_registration
            FROM trips t
            LEFT JOIN operators o ON o.id = t.operator_id
            LEFT JOIN vehicles  v ON v.id = t.vehicle_id
            """;

    public List<Trip> findAll(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return jdbc.query(BASE_SELECT + " ORDER BY t.id", MAPPER);
        }
        return jdbc.query(BASE_SELECT + " WHERE t.status = ? ORDER BY t.id", MAPPER, statusFilter);
    }

    public List<Trip> findActive() {
        return jdbc.query(
                BASE_SELECT + " WHERE t.status IN ('on-route','delayed','deviated') ORDER BY t.id",
                MAPPER
        );
    }

    public List<Trip> findByOperator(Long operatorId) {
        return jdbc.query(
                BASE_SELECT + " WHERE t.operator_id = ? ORDER BY t.id DESC",
                MAPPER, operatorId
        );
    }

    public Optional<Trip> findById(Long id) {
        List<Trip> rows = jdbc.query(BASE_SELECT + " WHERE t.id = ?", MAPPER, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Long insert(Trip t) {
        String sql = """
                INSERT INTO trips
                  (operator_id, vehicle_id, route_id, route_name,
                   departure_time, status, current_lat, current_lng)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, t.getOperatorId());
            ps.setLong(2, t.getVehicleId());

            if (t.getRouteId() == null) ps.setNull(3, java.sql.Types.BIGINT);
            else ps.setLong(3, t.getRouteId());

            ps.setString(4, t.getRouteName());

            if (t.getDepartureTime() == null) ps.setNull(5, java.sql.Types.TIMESTAMP);
            else ps.setTimestamp(5, Timestamp.valueOf(t.getDepartureTime()));

            ps.setString(6, t.getStatusString());

            if (t.getCurrentLat() == null) ps.setNull(7, java.sql.Types.DECIMAL);
            else ps.setBigDecimal(7, t.getCurrentLat());

            if (t.getCurrentLng() == null) ps.setNull(8, java.sql.Types.DECIMAL);
            else ps.setBigDecimal(8, t.getCurrentLng());

            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public int updateStatus(Long id, String status) {
        return jdbc.update("UPDATE trips SET status = ? WHERE id = ?", status, id);
    }

    public int count() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM trips", Integer.class);
        return n == null ? 0 : n;
    }
}
