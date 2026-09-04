package com.kaung.uit_virtual_campus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaung.uit_virtual_campus.dto.EventCreateRequest;
import com.kaung.uit_virtual_campus.dto.EventResponse;
import com.kaung.uit_virtual_campus.dto.EventUpdateRequest;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @Test
    @DisplayName("Unauthenticated event list returns 401")
    void unauthenticatedListReturns401() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can list events with filters")
    void authenticatedCanListEvents() throws Exception {
        when(eventService.listEvents(eq(10L), any(OffsetDateTime.class), any(OffsetDateTime.class), eq("Multipurpose Hall")))
                .thenReturn(List.of(eventResponse()));

        mockMvc.perform(get("/api/events")
                        .param("clubId", "10")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-30T00:00:00Z")
                        .param("location", "Multipurpose Hall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(50))
                .andExpect(jsonPath("$[0].location").value("Multipurpose Hall"));
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can list upcoming Multipurpose Hall events")
    void authenticatedCanListUpcomingHallEvents() throws Exception {
        when(eventService.getUpcomingEvents(eq(null), anyString())).thenReturn(List.of(eventResponse()));

        mockMvc.perform(get("/api/events/upcoming?location=Multipurpose%20Hall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(50))
                .andExpect(jsonPath("$[0].location").value("Multipurpose Hall"));
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can get event detail")
    void authenticatedCanGetEvent() throws Exception {
        when(eventService.getEvent(50L)).thenReturn(eventResponse());

        mockMvc.perform(get("/api/events/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Campus Event"));
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated valid writer can create event")
    void authenticatedWriterCanCreateEvent() throws Exception {
        EventCreateRequest request = createRequest(10L);
        when(eventService.createEvent(any(EventCreateRequest.class))).thenReturn(eventResponse());

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(50));
    }

    @Test
    @WithMockUser
    @DisplayName("Unauthorized write returns 403")
    void unauthorizedWriteReturns403() throws Exception {
        when(eventService.createEvent(any(EventCreateRequest.class))).thenThrow(new AccessDeniedException("denied"));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser
    @DisplayName("Validation errors return 400")
    void validationErrorsReturn400() throws Exception {
        EventCreateRequest request = new EventCreateRequest(10L, "", null,
                OffsetDateTime.parse("2026-09-01T09:00:00Z"),
                OffsetDateTime.parse("2026-09-01T10:00:00Z"),
                null,
                "not-a-url");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.imageUrl").exists());

        verifyNoInteractions(eventService);
    }

    @Test
    @WithMockUser
    @DisplayName("Valid writer can update event without club id")
    void validWriterCanUpdateEvent() throws Exception {
        EventUpdateRequest request = new EventUpdateRequest(
                "Campus Event",
                "Desc",
                OffsetDateTime.parse("2026-09-01T09:00:00Z"),
                OffsetDateTime.parse("2026-09-01T10:00:00Z"),
                "Multipurpose Hall",
                null
        );
        when(eventService.updateEvent(eq(50L), any(EventUpdateRequest.class))).thenReturn(eventResponse());

        mockMvc.perform(put("/api/events/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(50));
    }

    @Test
    @WithMockUser
    @DisplayName("Missing event returns 404")
    void missingEventReturns404() throws Exception {
        when(eventService.getEvent(999L)).thenThrow(new ResourceNotFoundException("Event not found with id: 999"));

        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("Valid writer can delete event")
    void validWriterCanDeleteEvent() throws Exception {
        mockMvc.perform(delete("/api/events/50"))
                .andExpect(status().isNoContent());
    }

    private EventCreateRequest createRequest(Long clubId) {
        return new EventCreateRequest(
                clubId,
                "Campus Event",
                "Desc",
                OffsetDateTime.parse("2026-09-01T09:00:00Z"),
                OffsetDateTime.parse("2026-09-01T10:00:00Z"),
                "Multipurpose Hall",
                "https://example.com/event.jpg"
        );
    }

    private EventResponse eventResponse() {
        return new EventResponse(
                50L,
                "Campus Event",
                "Desc",
                OffsetDateTime.parse("2026-09-01T09:00:00Z"),
                OffsetDateTime.parse("2026-09-01T10:00:00Z"),
                "Multipurpose Hall",
                null,
                UUID.randomUUID(),
                "Creator",
                10L,
                "Programming Club",
                null,
                null
        );
    }
}
