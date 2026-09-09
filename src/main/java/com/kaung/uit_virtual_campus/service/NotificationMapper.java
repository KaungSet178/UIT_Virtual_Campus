package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.NotificationResponse;
import com.kaung.uit_virtual_campus.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getEntityType(),
                notification.getEntityId(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
