package com.kaung.uit_virtual_campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "notifications", schema = "public")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type")
    private NotificationEntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    protected Notification() {
    }

    public Notification(User user, NotificationType type, String title, String message,
                        NotificationEntityType entityType, Long entityId) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.message = message;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public User getUser() {
        return user;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public NotificationEntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public boolean isRead() {
        return read;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getReadAt() {
        return readAt;
    }

    public void markRead() {
        if (!read) {
            read = true;
            readAt = OffsetDateTime.now();
        }
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Notification that)) {
            return false;
        }
        return notificationId != null && Objects.equals(notificationId, that.notificationId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
