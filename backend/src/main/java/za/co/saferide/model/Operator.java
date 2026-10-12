package za.co.saferide.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model for a row in the `operators` table.
 */
public class Operator {

    private Long id;
    private Long userId;
    private String companyName;
    private Long schoolId;
    private LocalDate prdpExpiry;
    private LocalDate roadworthyExpiry;
    private LocalDate registrationExpiry;
    private boolean active;
    private LocalDateTime createdAt;

    // Joined display fields
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String schoolName;

    public Operator() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public LocalDate getPrdpExpiry() { return prdpExpiry; }
    public void setPrdpExpiry(LocalDate prdpExpiry) { this.prdpExpiry = prdpExpiry; }

    public LocalDate getRoadworthyExpiry() { return roadworthyExpiry; }
    public void setRoadworthyExpiry(LocalDate roadworthyExpiry) { this.roadworthyExpiry = roadworthyExpiry; }

    public LocalDate getRegistrationExpiry() { return registrationExpiry; }
    public void setRegistrationExpiry(LocalDate registrationExpiry) { this.registrationExpiry = registrationExpiry; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
}
