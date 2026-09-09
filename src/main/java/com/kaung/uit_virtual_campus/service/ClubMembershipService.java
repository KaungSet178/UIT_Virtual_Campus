package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ClubMemberManagementResponse;
import com.kaung.uit_virtual_campus.dto.ClubMemberResponse;
import com.kaung.uit_virtual_campus.dto.MyClubResponse;
import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.ClubMembership;
import com.kaung.uit_virtual_campus.entity.ClubMembershipStatus;
import com.kaung.uit_virtual_campus.entity.NotificationEntityType;
import com.kaung.uit_virtual_campus.entity.NotificationType;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.ClubMembershipRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ClubMembershipService {

    private final ClubService clubService;
    private final ClubMembershipRepository clubMembershipRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CurrentUserService currentUserService;
    private final ClubAuthorizationService authorizationService;
    private final ClubMapper clubMapper;
    private final NotificationService notificationService;

    public ClubMembershipService(
            ClubService clubService,
            ClubMembershipRepository clubMembershipRepository,
            StudentProfileRepository studentProfileRepository,
            CurrentUserService currentUserService,
            ClubAuthorizationService authorizationService,
            ClubMapper clubMapper,
            NotificationService notificationService
    ) {
        this.clubService = clubService;
        this.clubMembershipRepository = clubMembershipRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.clubMapper = clubMapper;
        this.notificationService = notificationService;
    }

    public List<ClubMemberResponse> getActiveMembers(Long clubId) {
        clubService.findClubWithLeader(clubId);
        return clubMembershipRepository.findByClubIdAndStatusWithDetails(clubId, ClubMembershipStatus.ACTIVE).stream()
                .map(clubMapper::toPublicMemberResponse)
                .toList();
    }

    public List<ClubMemberManagementResponse> getMembersForManagement(Long clubId) {
        User currentUser = currentUserService.getCurrentUser();
        Club club = clubService.findClubWithLeader(clubId);
        authorizationService.requireClubManager(currentUser, club);
        return clubMembershipRepository.findByClubIdAndStatusWithDetails(clubId, ClubMembershipStatus.ACTIVE).stream()
                .map(clubMapper::toManagementMemberResponse)
                .toList();
    }

    public List<MyClubResponse> getMyClubs() {
        UUID currentUserId = currentUserService.getCurrentUserId();
        return clubMembershipRepository.findByStudentIdWithDetails(currentUserId).stream()
                .map(membership -> clubMapper.toMyClubResponse(membership, currentUserId))
                .toList();
    }

    @Transactional
    public MyClubResponse joinClub(Long clubId) {
        User currentUser = currentUserService.getCurrentUser();
        authorizationService.requireStudent(currentUser);
        StudentProfile studentProfile = getStudentProfile(currentUser.getUserId());
        Club club = clubService.findClubWithLeader(clubId);

        ClubMembership membership = clubMembershipRepository
                .findByClubIdAndStudentIdWithDetails(clubId, currentUser.getUserId())
                .map(existing -> reactivateMembership(existing, clubId))
                .orElseGet(() -> clubMembershipRepository.save(new ClubMembership(club, studentProfile)));

        notifyLeader(club, NotificationType.CLUB_MEMBER_JOINED, "New Club Member",
                displayName(currentUser) + " joined " + club.getClubName() + ".");
        return clubMapper.toMyClubResponse(membership, currentUser.getUserId());
    }

    @Transactional
    public MyClubResponse leaveClub(Long clubId) {
        User currentUser = currentUserService.getCurrentUser();
        authorizationService.requireStudent(currentUser);
        Club club = clubService.findClubWithLeader(clubId);
        if (club.getLeader().getUserId().equals(currentUser.getUserId())) {
            throw new ConflictException("A club leader cannot leave their own club while they remain leader.");
        }

        ClubMembership membership = clubMembershipRepository
                .findByClubIdAndStudentIdWithDetails(clubId, currentUser.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Club membership not found for current user."));

        if (membership.getStatus() == ClubMembershipStatus.LEFT) {
            throw new ConflictException("User has already left this club.");
        }
        if (membership.getStatus() == ClubMembershipStatus.SUSPENDED) {
            throw new ConflictException("Suspended memberships cannot be left by the student.");
        }

        membership.leave();
        notifyLeader(club, NotificationType.CLUB_MEMBER_LEFT, "Club Member Left",
                displayName(currentUser) + " left " + club.getClubName() + ".");
        return clubMapper.toMyClubResponse(membership, currentUser.getUserId());
    }

    private ClubMembership reactivateMembership(ClubMembership membership, Long clubId) {
        if (membership.getStatus() == ClubMembershipStatus.ACTIVE) {
            throw new ConflictException("User is already an active member of club id: " + clubId);
        }
        if (membership.getStatus() == ClubMembershipStatus.SUSPENDED) {
            throw new ConflictException("Suspended memberships cannot be reactivated by the student.");
        }
        membership.activate();
        return membership;
    }

    private StudentProfile getStudentProfile(UUID studentId) {
        return studentProfileRepository.findWithUserByStudentId(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for current user."));
    }

    private void notifyLeader(Club club, NotificationType type, String title, String message) {
        notificationService.notifyUser(
                club.getLeader().getUserId(),
                type,
                title,
                message,
                NotificationEntityType.CLUB,
                club.getClubId()
        );
    }

    private String displayName(User user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
    }
}
