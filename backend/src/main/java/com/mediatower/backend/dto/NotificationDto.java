package com.mediatower.backend.dto;
import java.time.LocalDateTime;
public class NotificationDto {
    private Long id;
    private String userId;
    private String message;
    private String type;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String link;
    public NotificationDto() {}
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getLink() { return link; }
    public void setId(Long id) { this.id = id; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setMessage(String message) { this.message = message; }
    public void setType(String type) { this.type = type; }
    public void setRead(boolean read) { isRead = read; }
    public void setIsRead(boolean read) { isRead = read; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setLink(String link) { this.link = link; }
}