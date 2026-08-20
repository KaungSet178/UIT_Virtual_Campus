package com.kaung.uit_virtual_campus.dto;

import java.time.LocalTime;

public record AvailabilityConflictResponse(
        String type,
        LocalTime startTime,
        LocalTime endTime,
        String description
) {
}
