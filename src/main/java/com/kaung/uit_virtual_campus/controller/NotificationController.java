package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.NotificationResponse;
import com.kaung.uit_virtual_campus.dto.UnreadNotificationCountResponse;
import com.kaung.uit_virtual_campus.service.CurrentUserService;
import com.kaung.uit_virtual_campus.service.NotificationService;
import com.kaung.uit_virtual_campus.service.NotificationSseService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final NotificationSseService sseService;

    public NotificationController(
            NotificationService notificationService,
            CurrentUserService currentUserService,
            NotificationSseService sseService
    ) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
        this.sseService = sseService;
    }

    @GetMapping
    public List<NotificationResponse> getNotifications() {
        return notificationService.getCurrentUserNotifications();
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse getUnreadCount() {
        return new UnreadNotificationCountResponse(notificationService.getUnreadCount());
    }

    @PatchMapping("/{notificationId}/read")
    public NotificationResponse markRead(@PathVariable Long notificationId) {
        return notificationService.markRead(notificationId);
    }

    @PatchMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead();
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseService.subscribe(currentUserService.getCurrentUserId());
    }
}
