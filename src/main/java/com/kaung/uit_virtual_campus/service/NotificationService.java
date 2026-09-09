package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.NotificationResponse;
import com.kaung.uit_virtual_campus.entity.Notification;
import com.kaung.uit_virtual_campus.entity.NotificationEntityType;
import com.kaung.uit_virtual_campus.entity.NotificationType;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.NotificationRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final NotificationMapper notificationMapper;
    private final NotificationSseService sseService;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            NotificationMapper notificationMapper,
            NotificationSseService sseService
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.notificationMapper = notificationMapper;
        this.sseService = sseService;
    }

    @Transactional
    public NotificationResponse notifyUser(UUID userId, NotificationType type, String title, String message,
                                           NotificationEntityType entityType, Long entityId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Notification notification = notificationRepository.save(new Notification(
                user,
                type,
                title,
                message,
                entityType,
                entityId
        ));
        NotificationResponse response = notificationMapper.toResponse(notification);
        sendAfterCommit(userId, response);
        return response;
    }

    @Transactional
    public List<NotificationResponse> notifyUsers(Collection<UUID> userIds, NotificationType type, String title,
                                                  String message, NotificationEntityType entityType, Long entityId) {
        Set<UUID> uniqueUserIds = new LinkedHashSet<>(userIds);
        return uniqueUserIds.stream()
                .map(userId -> notifyUser(userId, type, title, message, entityType, entityId))
                .toList();
    }

    public List<NotificationResponse> getCurrentUserNotifications() {
        UUID currentUserId = currentUserService.getCurrentUserId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    public long getUnreadCount() {
        return notificationRepository.countByUserUserIdAndReadFalse(currentUserService.getCurrentUserId());
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        notification.markRead();
        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead(currentUserService.getCurrentUserId(), OffsetDateTime.now());
    }

    private void sendAfterCommit(UUID userId, NotificationResponse response) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            sseService.send(userId, response);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sseService.send(userId, response);
            }
        });
    }
}
