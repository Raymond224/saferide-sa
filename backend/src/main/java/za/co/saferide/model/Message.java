package za.co.saferide.model;

import java.time.LocalDateTime;

/**
 * Model for a row in the `messages` table.
 * Represents a parent-to-school message (or a school reply).
 */
public class Message {

    private Long id;
    private Long fromUser;
    private Long toSchool;
    private String content;
    private Long replyTo;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    // Joined display fields
    private String fromUserName;
    private String fromUserRole;
    private String schoolName;

    public Message() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFromUser() { return fromUser; }
    public void setFromUser(Long fromUser) { this.fromUser = fromUser; }

    public Long getToSchool() { return toSchool; }
    public void setToSchool(Long toSchool) { this.toSchool = toSchool; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getReplyTo() { return replyTo; }
    public void setReplyTo(Long replyTo) { this.replyTo = replyTo; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFromUserName() { return fromUserName; }
    public void setFromUserName(String fromUserName) { this.fromUserName = fromUserName; }

    public String getFromUserRole() { return fromUserRole; }
    public void setFromUserRole(String fromUserRole) { this.fromUserRole = fromUserRole; }

    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
}
