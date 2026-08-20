package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ClubGalleryResponse;
import com.kaung.uit_virtual_campus.dto.ClubMemberManagementResponse;
import com.kaung.uit_virtual_campus.dto.ClubMemberResponse;
import com.kaung.uit_virtual_campus.dto.ClubResponse;
import com.kaung.uit_virtual_campus.dto.MyClubResponse;
import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.ClubGallery;
import com.kaung.uit_virtual_campus.entity.ClubMembership;
import com.kaung.uit_virtual_campus.entity.ClubMembershipStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ClubMapper {

    public ClubResponse toClubResponse(Club club, long activeMemberCount) {
        return new ClubResponse(
                club.getClubId(),
                club.getClubName(),
                club.getDescription(),
                club.getContactEmail(),
                club.getContactPhone(),
                club.getLogoUrl(),
                club.getLeader().getUserId(),
                club.getLeader().getFullName(),
                activeMemberCount,
                club.getCreatedAt(),
                club.getUpdatedAt()
        );
    }

    public ClubMemberResponse toPublicMemberResponse(ClubMembership membership) {
        return new ClubMemberResponse(
                membership.getMembershipId(),
                membership.getStudent().getUser().getFullName(),
                membership.getJoinedAt()
        );
    }

    public ClubMemberManagementResponse toManagementMemberResponse(ClubMembership membership) {
        return new ClubMemberManagementResponse(
                membership.getMembershipId(),
                membership.getStudent().getUser().getUserId(),
                membership.getStudent().getUser().getFullName(),
                membership.getStudent().getUser().getEmail(),
                membership.getStudent().getStudentNumber(),
                membership.getStudent().getDepartment(),
                membership.getStudent().getYear(),
                membership.getStatus(),
                membership.getJoinedAt()
        );
    }

    public MyClubResponse toMyClubResponse(ClubMembership membership, UUID currentUserId) {
        return new MyClubResponse(
                membership.getClub().getClubId(),
                membership.getClub().getClubName(),
                membership.getClub().getLogoUrl(),
                membership.getClub().getLeader().getUserId().equals(currentUserId),
                membership.getStatus(),
                membership.getJoinedAt()
        );
    }

    public ClubGalleryResponse toGalleryResponse(ClubGallery gallery) {
        return new ClubGalleryResponse(
                gallery.getGalleryId(),
                gallery.getClub().getClubId(),
                gallery.getImageUrl(),
                gallery.getCaption(),
                gallery.getUploadedAt()
        );
    }

    public boolean isActive(ClubMembership membership) {
        return membership.getStatus() == ClubMembershipStatus.ACTIVE;
    }
}
