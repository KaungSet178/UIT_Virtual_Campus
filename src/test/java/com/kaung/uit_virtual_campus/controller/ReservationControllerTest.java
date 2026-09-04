package com.kaung.uit_virtual_campus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaung.uit_virtual_campus.dto.ReservationCreateRequest;
import com.kaung.uit_virtual_campus.dto.ReservationResponse;
import com.kaung.uit_virtual_campus.dto.RoomAvailabilityResponse;
import com.kaung.uit_virtual_campus.entity.ReservationStatus;
import com.kaung.uit_virtual_campus.service.ReservationService;
import com.kaung.uit_virtual_campus.service.RoomAvailabilityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private RoomAvailabilityService roomAvailabilityService;

    @Test
    @DisplayName("Unauthenticated create reservation returns 401")
    void unauthenticatedCreateReservationReturns401() throws Exception {
        ReservationCreateRequest request = new ReservationCreateRequest(
                10L,
                LocalDate.of(2026, 8, 24),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "Meeting"
        );

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can create reservation")
    void authenticatedUserCanCreateReservation() throws Exception {
        ReservationCreateRequest request = new ReservationCreateRequest(
                10L,
                LocalDate.of(2026, 8, 24),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "Meeting"
        );
        when(reservationService.createReservation(any(ReservationCreateRequest.class))).thenReturn(reservationResponse(30L));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(30))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can list own reservations")
    void authenticatedUserCanListOwnReservations() throws Exception {
        when(reservationService.getCurrentUserReservations()).thenReturn(List.of(reservationResponse(30L)));

        mockMvc.perform(get("/api/reservations/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reservationId").value(30));
    }

    @Test
    @WithMockUser
    @DisplayName("Non-admin cannot access admin reservations")
    void nonAdminCannotAccessAdminReservations() throws Exception {
        mockMvc.perform(get("/api/admin/reservations"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can approve reservation")
    void adminCanApproveReservation() throws Exception {
        ReservationResponse response = reservationResponse(30L, ReservationStatus.APPROVED);
        when(reservationService.approveReservation(30L)).thenReturn(response);

        mockMvc.perform(patch("/api/admin/reservations/30/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @WithMockUser
    @DisplayName("Availability endpoint requires authenticated user and returns redacted conflicts")
    void availabilityEndpointReturnsResponse() throws Exception {
        when(roomAvailabilityService.getAvailability(
                10L,
                LocalDate.of(2026, 8, 24),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        )).thenReturn(new RoomAvailabilityResponse(
                10L,
                LocalDate.of(2026, 8, 24),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                true,
                List.of()
        ));

        mockMvc.perform(get("/api/rooms/10/availability")
                        .param("date", "2026-08-24")
                        .param("startTime", "09:00")
                        .param("endTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Availability endpoint reports missing request parameters with common envelope")
    void missingAvailabilityParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/rooms/10/availability")
                        .param("date", "2026-08-24")
                        .param("startTime", "09:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required request parameter: endTime"));
    }

    @Test
    @WithMockUser
    @DisplayName("Availability endpoint reports invalid date parameters with common envelope")
    void invalidDateParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/rooms/10/availability")
                        .param("date", "not-a-date")
                        .param("startTime", "09:00")
                        .param("endTime", "10:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for request parameter: date"));
    }

    @Test
    @WithMockUser
    @DisplayName("Malformed JSON request body returns safe common envelope")
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }

    private ReservationResponse reservationResponse(Long reservationId) {
        return reservationResponse(reservationId, ReservationStatus.PENDING);
    }

    private ReservationResponse reservationResponse(Long reservationId, ReservationStatus status) {
        return new ReservationResponse(
                reservationId,
                UUID.randomUUID(),
                "user@example.com",
                "User",
                10L,
                "111",
                "Room 111",
                1L,
                "Building 1",
                101L,
                1,
                LocalDate.of(2026, 8, 24),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                "Meeting",
                status,
                null,
                null,
                null,
                null,
                null
        );
    }
}
