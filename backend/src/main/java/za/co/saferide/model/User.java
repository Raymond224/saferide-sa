package za.co.saferide.model;

import java.time.LocalDateTime;

/**
 * Model for a row in the `users` table.
 * Includes role enum matching the DB ENUM('parent','operator','admin','system-admin').
 */
public class User {

    public enum Role {
        PARENT("parent"),
        OPERATOR("operator"),
        ADMIN("admin"),
        SYSTEM_ADMIN("system-admin");

        private final String dbValue;

        Role(String dbValue) { this.dbValue = dbValue; }

        public String getDbValue() { return dbValue; }

        public static Role fromDb(String value) {
            for (Role r : values()) {
                if (r.dbValue.equals(value)) return r;
            }
            throw new IllegalArgumentException("Unknown role: " + value);
        }
    }

    private Long id;
    private String username;
    private String passwordHash;
    private Role role;
    private String fullName;
    private String email;
    private String phone;
    private Long schoolId;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public User() {}

    // ----- Getters & setters -----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    /** Convenience: role as stored in the DB. */
    public String getRoleString() { return role == null ? null : role.getDbValue(); }
}
