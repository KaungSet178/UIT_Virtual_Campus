package com.kaung.uit_virtual_campus.dto;

import com.kaung.uit_virtual_campus.entity.ClubMembershipStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClubMemberManagementResponse(
        Long membershipId,
        UUID userId,
        String fullName,
        String email,
        String studentNumber,
        String department,
        Short year,
        ClubMembershipStatus status,
        OffsetDateTime joinedAt
) {
}
