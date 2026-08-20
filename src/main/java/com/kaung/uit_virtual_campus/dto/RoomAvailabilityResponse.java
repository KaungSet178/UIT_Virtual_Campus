package com.kaung.uit_virtual_campus.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record RoomAvailabilityResponse(
        Long roomId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        boolean available,
        List<AvailabilityConflictResponse> conflicts
) {
}
