package za.co.saferide.model;

import java.time.LocalDateTime;

/**
 * Model for a row in the `incidents` table.
 */
public class Incident {

    public enum Status {
        OPEN("open"),
        INVESTIGATING("investigating"),
        RESOLVED("resolved");

        private final String dbValue;
        Status(String dbValue) { this.dbValue = dbValue; }
        public String getDbValue() { return dbValue; }

        public static Status fromDb(String value) {
            for (Status s : values()) {
                if (s.dbValue.equals(value)) return s;
            }
            throw new IllegalArgumentException("Unknown incident status: " + value);
        }
    }

    private Long id;
    private Long reportedBy;
    private Long tripId;
    private String type;
    private String description;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    // Joined display fields (not columns of incidents)
    private String reporterName;
    private String reporterRole;
    private String tripRouteName;

    public Incident() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReportedBy() { return reportedBy; }
    public void setReportedBy(Long reportedBy) { this.reportedBy = reportedBy; }

    public Long getTripId() { return tripId; }
    public void setTripId(Long tripId) { this.tripId = tripId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReporterRole() { return reporterRole; }
    public void setReporterRole(String reporterRole) { this.reporterRole = reporterRole; }

    public String getTripRouteName() { return tripRouteName; }
    public void setTripRouteName(String tripRouteName) { this.tripRouteName = tripRouteName; }

    public String getStatusString() { return status == null ? null : status.getDbValue(); }
}
