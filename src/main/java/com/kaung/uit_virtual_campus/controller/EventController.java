package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.EventCreateRequest;
import com.kaung.uit_virtual_campus.dto.EventResponse;
import com.kaung.uit_virtual_campus.dto.EventUpdateRequest;
import com.kaung.uit_virtual_campus.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventResponse> listEvents(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) String location
    ) {
        return eventService.listEvents(clubId, from, to, location);
    }

    @GetMapping("/upcoming")
    public List<EventResponse> listUpcomingEvents(
            @RequestParam(required = false) Long clubId,
            @RequestParam(required = false) String location
    ) {
        return eventService.getUpcomingEvents(clubId, location);
    }

    @GetMapping("/{eventId}")
    public EventResponse getEvent(@PathVariable Long eventId) {
        return eventService.getEvent(eventId);
    }

    @PostMapping
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity
                .created(URI.create("/api/events/" + response.eventId()))
                .body(response);
    }

    @PutMapping("/{eventId}")
    public EventResponse updateEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        return eventService.updateEvent(eventId, request);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build();
    }
}
