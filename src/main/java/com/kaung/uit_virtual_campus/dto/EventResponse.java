package com.kaung.uit_virtual_campus.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
        Long eventId,
        String title,
        String description,
        OffsetDateTime startDatetime,
        OffsetDateTime endDatetime,
        String location,
        String imageUrl,
        UUID createdById,
        String createdByName,
        Long clubId,
        String clubName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
