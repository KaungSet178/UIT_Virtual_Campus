package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.NotificationResponse;
import com.kaung.uit_virtual_campus.entity.Notification;
import com.kaung.uit_virtual_campus.entity.NotificationEntityType;
import com.kaung.uit_virtual_campus.entity.NotificationType;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.NotificationRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private NotificationSseService sseService;

    private NotificationService notificationService;
    private User user;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                userRepository,
                currentUserService,
                new NotificationMapper(),
                sseService
        );
        user = new User(UUID.randomUUID(), "user@example.com", "User", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of());
    }

    @Test
    @DisplayName("Creates unread notification")
    void createsUnreadNotification() {
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.notifyUser(
                user.getUserId(),
                NotificationType.EVENT_CREATED,
                "New Event",
                "Event message",
                NotificationEntityType.EVENT,
                50L
        );

        assertThat(response.isRead()).isFalse();
        assertThat(response.type()).isEqualTo(NotificationType.EVENT_CREATED);
        assertThat(response.entityType()).isEqualTo(NotificationEntityType.EVENT);
    }

    @Test
    @DisplayName("Lists and counts only current user's notifications")
    void listsCurrentUserNotifications() {
        Notification notification = new Notification(
                user,
                NotificationType.CLUB_MEMBER_JOINED,
                "New Club Member",
                "Student joined",
                NotificationEntityType.CLUB,
                10L
        );
        when(currentUserService.getCurrentUserId()).thenReturn(user.getUserId());
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId())).thenReturn(List.of(notification));
        when(notificationRepository.countByUserUserIdAndReadFalse(user.getUserId())).thenReturn(1L);

        assertThat(notificationService.getCurrentUserNotifications()).hasSize(1);
        assertThat(notificationService.getUnreadCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Marks current user's notification read")
    void marksOwnNotificationRead() {
        Notification notification = new Notification(
                user,
                NotificationType.RESERVATION_APPROVED,
                "Reservation Approved",
                "Approved",
                NotificationEntityType.RESERVATION,
                30L
        );
        when(currentUserService.getCurrentUserId()).thenReturn(user.getUserId());
        when(notificationRepository.findByIdAndUserId(5L, user.getUserId())).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.markRead(5L);

        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    @DisplayName("Wrong user cannot mark another user's notification read")
    void wrongUserCannotMarkRead() {
        when(currentUserService.getCurrentUserId()).thenReturn(user.getUserId());
        when(notificationRepository.findByIdAndUserId(5L, user.getUserId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markRead(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Mark all read scopes update to current user")
    void markAllReadScopesToCurrentUser() {
        when(currentUserService.getCurrentUserId()).thenReturn(user.getUserId());

        notificationService.markAllRead();

        verify(notificationRepository).markAllRead(org.mockito.Mockito.eq(user.getUserId()), any());
    }
}
