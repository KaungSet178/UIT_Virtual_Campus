package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ClubCreateRequest;
import com.kaung.uit_virtual_campus.dto.ClubResponse;
import com.kaung.uit_virtual_campus.dto.ClubUpdateRequest;
import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.ClubMembership;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.repository.ClubMembershipRepository;
import com.kaung.uit_virtual_campus.repository.ClubRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private ClubMembershipRepository clubMembershipRepository;

    @Mock
    private CurrentUserService currentUserService;

    private ClubService clubService;
    private User leader;
    private User admin;
    private StudentProfile leaderProfile;
    private Club club;

    @BeforeEach
    void setUp() {
        ClubAuthorizationService auth = new ClubAuthorizationService();
        ClubMapper mapper = new ClubMapper();
        clubService = new ClubService(
                clubRepository,
                userRepository,
                studentProfileRepository,
                clubMembershipRepository,
                currentUserService,
                auth,
                mapper
        );
        Role studentRole = new Role((short) 1, "STUDENT");
        Role leaderRole = new Role((short) 2, "CLUB_LEADER");
        Role adminRole = new Role((short) 3, "ADMIN");
        leader = new User(UUID.randomUUID(), "leader@example.com", "Leader", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(studentRole, leaderRole));
        admin = new User(UUID.randomUUID(), "admin@example.com", "Admin", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(adminRole));
        leaderProfile = new StudentProfile(leader, "S001", "CS", (short) 1);
        club = new Club(10L, leader, "Programming Club", "Desc", "club@example.com", null, null, null, null);
    }

    @Test
    @DisplayName("Admin create club validates leader and creates active leader membership")
    void createClubCreatesLeaderMembership() {
        ClubCreateRequest request = new ClubCreateRequest(leader.getUserId(), "Programming Club", "Desc", null, null, null);
        when(userRepository.findWithRolesByUserId(leader.getUserId())).thenReturn(Optional.of(leader));
        when(studentProfileRepository.findWithUserByStudentId(leader.getUserId())).thenReturn(Optional.of(leaderProfile));
        when(clubRepository.existsByClubNameIgnoreCase("Programming Club")).thenReturn(false);
        when(clubRepository.save(any(Club.class))).thenReturn(club);
        when(clubMembershipRepository.findByClubIdAndStudentIdWithDetails(10L, leader.getUserId())).thenReturn(Optional.empty());

        ClubResponse response = clubService.createClub(request);

        assertThat(response.clubId()).isEqualTo(10L);
        verify(clubMembershipRepository).save(any(ClubMembership.class));
    }

    @Test
    @DisplayName("Club list uses aggregate active member counts")
    void clubListUsesAggregateActiveMemberCounts() {
        when(clubRepository.findAllWithLeader()).thenReturn(java.util.List.of(club));
        when(clubMembershipRepository.countByClubIdsAndStatus(
                java.util.List.of(10L),
                com.kaung.uit_virtual_campus.entity.ClubMembershipStatus.ACTIVE
        )).thenReturn(java.util.List.of(new CountProjection(10L, 2L)));

        java.util.List<ClubResponse> responses = clubService.getAllClubs();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).activeMemberCount()).isEqualTo(2L);
        verify(clubMembershipRepository, never()).countByClubClubIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("Create club rejects leader without CLUB_LEADER role")
    void createClubRejectsInvalidLeader() {
        User studentOnly = new User(UUID.randomUUID(), "student@example.com", "Student", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(new Role((short) 1, "STUDENT")));
        ClubCreateRequest request = new ClubCreateRequest(studentOnly.getUserId(), "Programming Club", "Desc", null, null, null);
        when(userRepository.findWithRolesByUserId(studentOnly.getUserId())).thenReturn(Optional.of(studentOnly));

        assertThatThrownBy(() -> clubService.createClub(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ROLE_CLUB_LEADER");
    }

    @Test
    @DisplayName("Exact leader can update own club")
    void leaderCanUpdateOwnClub() {
        ClubUpdateRequest request = new ClubUpdateRequest("Programming Club", "Updated", null, null, null);
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(clubRepository.findByIdWithLeader(10L)).thenReturn(Optional.of(club));

        ClubResponse response = clubService.updateClub(10L, request);

        assertThat(response.description()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("Other club leader cannot update another leader's club")
    void otherLeaderCannotUpdateClub() {
        User otherLeader = new User(UUID.randomUUID(), "other@example.com", "Other", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(new Role((short) 2, "CLUB_LEADER")));
        ClubUpdateRequest request = new ClubUpdateRequest("Programming Club", "Updated", null, null, null);
        when(currentUserService.getCurrentUser()).thenReturn(otherLeader);
        when(clubRepository.findByIdWithLeader(10L)).thenReturn(Optional.of(club));

        assertThatThrownBy(() -> clubService.updateClub(10L, request))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Duplicate club name is rejected")
    void duplicateClubNameRejected() {
        ClubUpdateRequest request = new ClubUpdateRequest("Another Club", "Updated", null, null, null);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(clubRepository.findByIdWithLeader(10L)).thenReturn(Optional.of(club));
        when(clubRepository.existsByClubNameIgnoreCase("Another Club")).thenReturn(true);

        assertThatThrownBy(() -> clubService.updateClub(10L, request))
                .isInstanceOf(ConflictException.class);
    }

    private record CountProjection(Long clubId, long activeMemberCount)
            implements ClubMembershipRepository.ClubMemberCount {

        @Override
        public Long getClubId() {
            return clubId;
        }

        @Override
        public long getActiveMemberCount() {
            return activeMemberCount;
        }
    }
}
