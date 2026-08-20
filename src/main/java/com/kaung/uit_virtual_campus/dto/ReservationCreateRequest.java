package com.kaung.uit_virtual_campus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationCreateRequest(
        @NotNull Long roomId,
        @NotNull LocalDate reservationDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotBlank @Size(max = 500) String purpose
) {
}
