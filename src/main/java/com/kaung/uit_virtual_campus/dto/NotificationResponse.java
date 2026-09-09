package com.kaung.uit_virtual_campus.dto;

import com.kaung.uit_virtual_campus.entity.NotificationEntityType;
import com.kaung.uit_virtual_campus.entity.NotificationType;

import java.time.OffsetDateTime;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String message,
        NotificationEntityType entityType,
        Long entityId,
        boolean isRead,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
) {
}
