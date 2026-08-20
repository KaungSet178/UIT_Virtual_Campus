package com.kaung.uit_virtual_campus.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClubResponse(
        Long clubId,
        String clubName,
        String description,
        String contactEmail,
        String contactPhone,
        String logoUrl,
        UUID leaderId,
        String leaderName,
        long activeMemberCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
