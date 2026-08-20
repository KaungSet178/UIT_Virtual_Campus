package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.MyClubResponse;
import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.ClubMembership;
import com.kaung.uit_virtual_campus.entity.ClubMembershipStatus;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.repository.ClubMembershipRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubMembershipServiceTest {

    @Mock
    private ClubService clubService;

    @Mock
    private ClubMembershipRepository clubMembershipRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private CurrentUserService currentUserService;

    private ClubMembershipService membershipService;
    private User student;
    private User leader;
    private StudentProfile studentProfile;
    private Club club;

    @BeforeEach
    void setUp() {
        ClubAuthorizationService auth = new ClubAuthorizationService();
        ClubMapper mapper = new ClubMapper();
        membershipService = new ClubMembershipService(
                clubService,
                clubMembershipRepository,
                studentProfileRepository,
                currentUserService,
                auth,
                mapper
        );
        Role studentRole = new Role((short) 1, "STUDENT");
        Role leaderRole = new Role((short) 2, "CLUB_LEADER");
        student = new User(UUID.randomUUID(), "student@example.com", "Student", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(studentRole));
        leader = new User(UUID.randomUUID(), "leader@example.com", "Leader", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(studentRole, leaderRole));
        studentProfile = new StudentProfile(student, "S001", "CS", (short) 1);
        club = new Club(10L, leader, "Programming Club", "Desc", null, null, null, null, null);
    }

    @Test
    @DisplayName("Student can join club")
    void studentCanJoinClub() {
        ClubMembership membership = new ClubMembership(100L, club, studentProfile, null, ClubMembershipStatus.ACTIVE);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(studentProfileRepository.findWithUserByStudentId(student.getUserId())).thenReturn(Optional.of(studentProfile));
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(clubMembershipRepository.findByClubIdAndStudentIdWithDetails(10L, student.getUserId())).thenReturn(Optional.empty());
        when(clubMembershipRepository.save(any(ClubMembership.class))).thenReturn(membership);

        MyClubResponse response = membershipService.joinClub(10L);

        assertThat(response.membershipStatus()).isEqualTo(ClubMembershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("Non-student cannot join club")
    void nonStudentCannotJoinClub() {
        User nonStudent = new User(UUID.randomUUID(), "user@example.com", "User", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of());
        when(currentUserService.getCurrentUser()).thenReturn(nonStudent);

        assertThatThrownBy(() -> membershipService.joinClub(10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Left membership can rejoin")
    void leftMembershipCanRejoin() {
        ClubMembership membership = new ClubMembership(100L, club, studentProfile, null, ClubMembershipStatus.LEFT);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(studentProfileRepository.findWithUserByStudentId(student.getUserId())).thenReturn(Optional.of(studentProfile));
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(clubMembershipRepository.findByClubIdAndStudentIdWithDetails(10L, student.getUserId()))
                .thenReturn(Optional.of(membership));

        MyClubResponse response = membershipService.joinClub(10L);

        assertThat(response.membershipStatus()).isEqualTo(ClubMembershipStatus.ACTIVE);
    }

    @Test
    @DisplayName("Active membership cannot join again")
    void activeMembershipCannotJoinAgain() {
        ClubMembership membership = new ClubMembership(100L, club, studentProfile, null, ClubMembershipStatus.ACTIVE);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(studentProfileRepository.findWithUserByStudentId(student.getUserId())).thenReturn(Optional.of(studentProfile));
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(clubMembershipRepository.findByClubIdAndStudentIdWithDetails(10L, student.getUserId()))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> membershipService.joinClub(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already an active member");
    }

    @Test
    @DisplayName("Suspended membership cannot self-reactivate")
    void suspendedMembershipCannotRejoin() {
        ClubMembership membership = new ClubMembership(100L, club, studentProfile, null, ClubMembershipStatus.SUSPENDED);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(studentProfileRepository.findWithUserByStudentId(student.getUserId())).thenReturn(Optional.of(studentProfile));
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(clubMembershipRepository.findByClubIdAndStudentIdWithDetails(10L, student.getUserId()))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> membershipService.joinClub(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Suspended memberships cannot be reactivated");
    }

    @Test
    @DisplayName("Active member can leave club")
    void activeMemberCanLeaveClub() {
        ClubMembership membership = new ClubMembership(100L, club, studentProfile, null, ClubMembershipStatus.ACTIVE);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(clubMembershipRepository.findByClubIdAndStudentIdWithDetails(10L, student.getUserId()))
                .thenReturn(Optional.of(membership));

        MyClubResponse response = membershipService.leaveClub(10L);

        assertThat(response.membershipStatus()).isEqualTo(ClubMembershipStatus.LEFT);
    }

    @Test
    @DisplayName("Left membership cannot leave again")
    void leftMembershipCannotLeaveAgain() {
        ClubMembership membership = new ClubMembership(100L, club, studentProfile, null, ClubMembershipStatus.LEFT);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(clubMembershipRepository.findByClubIdAndStudentIdWithDetails(10L, student.getUserId()))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> membershipService.leaveClub(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already left");
    }

    @Test
    @DisplayName("Suspended membership cannot be left by student")
    void suspendedMembershipCannotLeave() {
        ClubMembership membership = new ClubMembership(100L, club, studentProfile, null, ClubMembershipStatus.SUSPENDED);
        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(clubMembershipRepository.findByClubIdAndStudentIdWithDetails(10L, student.getUserId()))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> membershipService.leaveClub(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Suspended memberships cannot be left");
    }

    @Test
    @DisplayName("Leader cannot leave own club")
    void leaderCannotLeaveOwnClub() {
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);

        assertThatThrownBy(() -> membershipService.leaveClub(10L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("leader cannot leave");
    }
}
