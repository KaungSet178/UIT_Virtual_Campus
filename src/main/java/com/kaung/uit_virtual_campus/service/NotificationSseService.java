package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.NotificationResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationSseService {

    private static final long TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(ignored -> removeEmitter(userId, emitter));

        sendEvent(userId, emitter, SseEmitter.event().name("connected").data("ok"));
        return emitter;
    }

    public void send(UUID userId, NotificationResponse notification) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            sendEvent(userId, emitter, SseEmitter.event().name("notification").data(notification));
        }
    }

    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        emitters.forEach((userId, userEmitters) -> {
            for (SseEmitter emitter : userEmitters) {
                sendEvent(userId, emitter, SseEmitter.event().name("heartbeat").data("keepalive"));
            }
        });
    }

    private void sendEvent(UUID userId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException exception) {
            removeEmitter(userId, emitter);
        }
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emitters.remove(userId, userEmitters);
        }
    }
}
