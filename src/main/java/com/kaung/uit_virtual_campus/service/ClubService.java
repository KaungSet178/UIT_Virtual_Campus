package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ClubCreateRequest;
import com.kaung.uit_virtual_campus.dto.ClubResponse;
import com.kaung.uit_virtual_campus.dto.ClubUpdateRequest;
import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.ClubMembership;
import com.kaung.uit_virtual_campus.entity.ClubMembershipStatus;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.ClubMembershipRepository;
import com.kaung.uit_virtual_campus.repository.ClubRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClubMembershipRepository clubMembershipRepository;
    private final CurrentUserService currentUserService;
    private final ClubAuthorizationService authorizationService;
    private final ClubMapper clubMapper;

    public ClubService(
            ClubRepository clubRepository,
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            ClubMembershipRepository clubMembershipRepository,
            CurrentUserService currentUserService,
            ClubAuthorizationService authorizationService,
            ClubMapper clubMapper
    ) {
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.clubMembershipRepository = clubMembershipRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.clubMapper = clubMapper;
    }

    public List<ClubResponse> getAllClubs() {
        List<Club> clubs = clubRepository.findAllWithLeader();
        Map<Long, Long> activeMemberCounts = activeMemberCountsByClubId(clubs);
        return clubs.stream()
                .map(club -> clubMapper.toClubResponse(
                        club,
                        activeMemberCounts.getOrDefault(club.getClubId(), 0L)
                ))
                .toList();
    }

    public ClubResponse getClubById(Long clubId) {
        return toClubResponse(findClubWithLeader(clubId));
    }

    @Transactional
    public ClubResponse createClub(ClubCreateRequest request) {
        User leader = userRepository.findWithRolesByUserId(request.leaderId())
                .orElseThrow(() -> new ResourceNotFoundException("Leader not found with id: " + request.leaderId()));
        StudentProfile leaderProfile = validateLeader(leader);
        validateUniqueClubName(request.clubName());

        try {
            Club saved = clubRepository.save(new Club(
                    leader,
                    normalize(request.clubName()),
                    trimToNull(request.description()),
                    trimToNull(request.contactEmail()),
                    trimToNull(request.contactPhone()),
                    trimToNull(request.logoUrl())
            ));
            ensureActiveLeaderMembership(saved, leaderProfile);
            return toClubResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Club already exists with name: " + request.clubName());
        }
    }

    @Transactional
    public ClubResponse updateClub(Long clubId, ClubUpdateRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Club club = findClubWithLeader(clubId);
        authorizationService.requireClubManager(currentUser, club);

        String nextName = normalize(request.clubName());
        if (!club.getClubName().equalsIgnoreCase(nextName) && clubRepository.existsByClubNameIgnoreCase(nextName)) {
            throw new ConflictException("Club already exists with name: " + nextName);
        }

        club.updateProfile(
                nextName,
                trimToNull(request.description()),
                trimToNull(request.contactEmail()),
                trimToNull(request.contactPhone()),
                trimToNull(request.logoUrl())
        );
        return toClubResponse(club);
    }

    @Transactional
    public void deleteClub(Long clubId) {
        if (!clubRepository.existsById(clubId)) {
            throw new ResourceNotFoundException("Club not found with id: " + clubId);
        }
        clubRepository.deleteById(clubId);
    }

    Club findClubWithLeader(Long clubId) {
        return clubRepository.findByIdWithLeader(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found with id: " + clubId));
    }

    private StudentProfile validateLeader(User leader) {
        if (!authorizationService.isStudent(leader) || !authorizationService.isClubLeader(leader)) {
            throw new IllegalArgumentException("Club leader must be a student with ROLE_CLUB_LEADER.");
        }
        return studentProfileRepository.findWithUserByStudentId(leader.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Club leader must have a student profile."));
    }

    private void validateUniqueClubName(String clubName) {
        String normalized = normalize(clubName);
        if (clubRepository.existsByClubNameIgnoreCase(normalized)) {
            throw new ConflictException("Club already exists with name: " + normalized);
        }
    }

    private void ensureActiveLeaderMembership(Club club, StudentProfile leaderProfile) {
        clubMembershipRepository.findByClubIdAndStudentIdWithDetails(club.getClubId(), leaderProfile.getStudentId())
                .ifPresentOrElse(existing -> {
                    if (existing.getStatus() == ClubMembershipStatus.SUSPENDED) {
                        throw new ConflictException("Suspended memberships cannot be reactivated for club leadership.");
                    }
                    existing.activate();
                }, () -> clubMembershipRepository.save(new ClubMembership(club, leaderProfile)));
    }

    private ClubResponse toClubResponse(Club club) {
        long activeMemberCount = clubMembershipRepository.countByClubClubIdAndStatus(
                club.getClubId(),
                ClubMembershipStatus.ACTIVE
        );
        return clubMapper.toClubResponse(club, activeMemberCount);
    }

    private Map<Long, Long> activeMemberCountsByClubId(List<Club> clubs) {
        List<Long> clubIds = clubs.stream()
                .map(Club::getClubId)
                .toList();
        if (clubIds.isEmpty()) {
            return Map.of();
        }
        return clubMembershipRepository.countByClubIdsAndStatus(clubIds, ClubMembershipStatus.ACTIVE).stream()
                .collect(Collectors.toMap(
                        ClubMembershipRepository.ClubMemberCount::getClubId,
                        ClubMembershipRepository.ClubMemberCount::getActiveMemberCount
                ));
    }

    private String normalize(String value) {
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
