package za.co.saferide.model;

import java.time.LocalDateTime;

/**
 * Model for a row in the `learners` table.
 * Also carries joined display fields describing the learner's most recent
 * trip_learners/trips assignment, so parent-facing screens can show a live
 * status per child without a second round trip.
 */
public class Learner {

    private Long id;
    private String fullName;
    private String grade;
    private Long parentId;
    private Long schoolId;
    private LocalDateTime createdAt;

    // Joined display fields
    private String schoolName;
    private Long currentTripId;
    private String currentTripStatus;   // trips.status db value, e.g. "on-route"
    private String currentRouteName;
    private LocalDateTime currentDepartureTime;
    private Boolean pickedUp;
    private Boolean droppedOff;
    private LocalDateTime pickedUpAt;
    private LocalDateTime droppedOffAt;

    public Learner() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }

    public Long getCurrentTripId() { return currentTripId; }
    public void setCurrentTripId(Long currentTripId) { this.currentTripId = currentTripId; }

    public String getCurrentTripStatus() { return currentTripStatus; }
    public void setCurrentTripStatus(String currentTripStatus) { this.currentTripStatus = currentTripStatus; }

    public String getCurrentRouteName() { return currentRouteName; }
    public void setCurrentRouteName(String currentRouteName) { this.currentRouteName = currentRouteName; }

    public LocalDateTime getCurrentDepartureTime() { return currentDepartureTime; }
    public void setCurrentDepartureTime(LocalDateTime currentDepartureTime) { this.currentDepartureTime = currentDepartureTime; }

    public Boolean getPickedUp() { return pickedUp; }
    public void setPickedUp(Boolean pickedUp) { this.pickedUp = pickedUp; }

    public Boolean getDroppedOff() { return droppedOff; }
    public void setDroppedOff(Boolean droppedOff) { this.droppedOff = droppedOff; }

    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public void setPickedUpAt(LocalDateTime pickedUpAt) { this.pickedUpAt = pickedUpAt; }

    public LocalDateTime getDroppedOffAt() { return droppedOffAt; }
    public void setDroppedOffAt(LocalDateTime droppedOffAt) { this.droppedOffAt = droppedOffAt; }
}
