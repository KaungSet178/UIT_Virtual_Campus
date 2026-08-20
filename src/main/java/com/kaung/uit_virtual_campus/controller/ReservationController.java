package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.ReservationCreateRequest;
import com.kaung.uit_virtual_campus.dto.ReservationResponse;
import com.kaung.uit_virtual_campus.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationCreateRequest request
    ) {
        ReservationResponse response = reservationService.createReservation(request);
        return ResponseEntity
                .created(URI.create("/api/reservations/" + response.reservationId()))
                .body(response);
    }

    @GetMapping("/me")
    public List<ReservationResponse> listMyReservations() {
        return reservationService.getCurrentUserReservations();
    }

    @GetMapping("/{reservationId}")
    public ReservationResponse getReservation(@PathVariable Long reservationId) {
        return reservationService.getReservationByIdForCurrentUser(reservationId);
    }

    @PatchMapping("/{reservationId}/cancel")
    public ReservationResponse cancelReservation(@PathVariable Long reservationId) {
        return reservationService.cancelReservation(reservationId);
    }
}
