package com.kaung.uit_virtual_campus.dto;

import java.time.OffsetDateTime;

public record ClubMemberResponse(
        Long membershipId,
        String fullName,
        OffsetDateTime joinedAt
) {
}
