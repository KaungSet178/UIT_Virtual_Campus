package com.kaung.uit_virtual_campus.dto;

import com.kaung.uit_virtual_campus.entity.ClubMembershipStatus;

import java.time.OffsetDateTime;

public record MyClubResponse(
        Long clubId,
        String clubName,
        String logoUrl,
        boolean leader,
        ClubMembershipStatus membershipStatus,
        OffsetDateTime joinedAt
) {
}
