package za.co.saferide.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Model for a row in the `trips` table.
 */
public class Trip {

    public enum Status {
        SCHEDULED("scheduled"),
        ON_ROUTE("on-route"),
        DELAYED("delayed"),
        DEVIATED("deviated"),
        COMPLETED("completed");

        private final String dbValue;
        Status(String dbValue) { this.dbValue = dbValue; }
        public String getDbValue() { return dbValue; }

        public static Status fromDb(String value) {
            for (Status s : values()) {
                if (s.dbValue.equals(value)) return s;
            }
            throw new IllegalArgumentException("Unknown trip status: " + value);
        }
    }

    private Long id;
    private Long operatorId;
    private Long vehicleId;
    private Long routeId;
    private String routeName;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Status status;
    private BigDecimal currentLat;
    private BigDecimal currentLng;
    private LocalDateTime createdAt;

    // Populated by joins
    private String operatorName;
    private String vehicleRegistration;

    public Trip() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }

    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public BigDecimal getCurrentLat() { return currentLat; }
    public void setCurrentLat(BigDecimal currentLat) { this.currentLat = currentLat; }

    public BigDecimal getCurrentLng() { return currentLng; }
    public void setCurrentLng(BigDecimal currentLng) { this.currentLng = currentLng; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public String getVehicleRegistration() { return vehicleRegistration; }
    public void setVehicleRegistration(String vehicleRegistration) { this.vehicleRegistration = vehicleRegistration; }

    public String getStatusString() { return status == null ? null : status.getDbValue(); }
}
