package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.ReservationResponse;
import com.kaung.uit_virtual_campus.entity.ReservationStatus;
import com.kaung.uit_virtual_campus.service.ReservationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/reservations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservationController {

    private final ReservationService reservationService;

    public AdminReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public List<ReservationResponse> listReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) LocalDate date
    ) {
        return reservationService.getReservationsForAdmin(status, roomId, userId, date);
    }

    @PatchMapping("/{reservationId}/approve")
    public ReservationResponse approveReservation(@PathVariable Long reservationId) {
        return reservationService.approveReservation(reservationId);
    }

    @PatchMapping("/{reservationId}/reject")
    public ReservationResponse rejectReservation(@PathVariable Long reservationId) {
        return reservationService.rejectReservation(reservationId);
    }
}
