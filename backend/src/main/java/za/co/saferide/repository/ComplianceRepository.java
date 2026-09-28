package za.co.saferide.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only compliance queries for the `operators` table and its expiry dates.
 * No separate table — we compute status from the operator's own expiry columns.
 */
@Repository
public class ComplianceRepository {

    private final JdbcTemplate jdbc;

    public ComplianceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** List every operator with its three expiry dates and a computed status. */
    public List<Map<String, Object>> listAll() {
        String sql = """
                SELECT o.id,
                       o.company_name,
                       o.prdp_expiry,
                       o.roadworthy_expiry,
                       o.registration_expiry,
                       s.name AS school_name
                FROM operators o
                LEFT JOIN schools s ON s.id = o.school_id
                WHERE o.active = 1
                ORDER BY o.company_name
                """;

        List<Map<String, Object>> raw = jdbc.queryForList(sql);
        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate soon = today.plusDays(30);

        for (Map<String, Object> row : raw) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", row.get("id"));
            out.put("companyName", row.get("company_name"));
            out.put("schoolName", row.get("school_name"));

            String prdpStatus = statusFor(row.get("prdp_expiry"), today, soon);
            String rwStatus = statusFor(row.get("roadworthy_expiry"), today, soon);
            String regStatus = statusFor(row.get("registration_expiry"), today, soon);

            out.put("prdpExpiry", toStr(row.get("prdp_expiry")));
            out.put("prdpStatus", prdpStatus);
            out.put("roadworthyExpiry", toStr(row.get("roadworthy_expiry")));
            out.put("roadworthyStatus", rwStatus);
            out.put("registrationExpiry", toStr(row.get("registration_expiry")));
            out.put("registrationStatus", regStatus);

            // Overall: red if any expired, amber if any expiring soon, green if all valid
            String overall;
            if ("expired".equals(prdpStatus) || "expired".equals(rwStatus) || "expired".equals(regStatus)) {
                overall = "non-compliant";
            } else if ("expiring".equals(prdpStatus) || "expiring".equals(rwStatus) || "expiring".equals(regStatus)) {
                overall = "expiring-soon";
            } else {
                overall = "compliant";
            }
            out.put("overall", overall);
            result.add(out);
        }
        return result;
    }

    /** Only operators with at least one expired or expiring document. */
    public List<Map<String, Object>> listAlerts() {
        List<Map<String, Object>> all = listAll();
        List<Map<String, Object>> alerts = new ArrayList<>();
        for (Map<String, Object> op : all) {
            if (!"compliant".equals(op.get("overall"))) {
                alerts.add(op);
            }
        }
        return alerts;
    }

    /** Summary counts for the dashboard. */
    public Map<String, Object> summary() {
        List<Map<String, Object>> all = listAll();
        long compliant = 0, expiring = 0, expired = 0;
        for (Map<String, Object> op : all) {
            String overall = (String) op.get("overall");
            if ("compliant".equals(overall)) compliant++;
            else if ("expiring-soon".equals(overall)) expiring++;
            else expired++;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalOperators", all.size());
        out.put("compliant", compliant);
        out.put("expiringSoon", expiring);
        out.put("nonCompliant", expired);
        out.put("totalAlerts", expiring + expired);
        return out;
    }

    // -------- helpers --------

    private String statusFor(Object dbValue, LocalDate today, LocalDate soon) {
        if (dbValue == null) return "unknown";
        LocalDate expiry = toLocalDate(dbValue);
        if (expiry == null) return "unknown";
        if (expiry.isBefore(today)) return "expired";
        if (expiry.isBefore(soon) || expiry.isEqual(soon)) return "expiring";
        return "valid";
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate) return (LocalDate) value;
        if (value instanceof Date) return ((Date) value).toLocalDate();
        if (value instanceof java.sql.Timestamp) return ((java.sql.Timestamp) value).toLocalDateTime().toLocalDate();
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String toStr(Object value) {
        if (value == null) return null;
        if (value instanceof Date) return value.toString();
        if (value instanceof LocalDate) return value.toString();
        return value.toString();
    }
}
