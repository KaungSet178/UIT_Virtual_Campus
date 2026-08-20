package com.kaung.uit_virtual_campus.dto;

import com.kaung.uit_virtual_campus.entity.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationResponse(
        Long reservationId,
        UUID userId,
        String userEmail,
        String userFullName,
        Long roomId,
        String roomNumber,
        String roomName,
        Long buildingId,
        String buildingName,
        Long floorId,
        Integer floorNumber,
        LocalDate reservationDate,
        LocalTime startTime,
        LocalTime endTime,
        String purpose,
        ReservationStatus status,
        UUID approvedByUserId,
        String approvedByEmail,
        String approvedByFullName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
