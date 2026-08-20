package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.ClubGallery;
import com.kaung.uit_virtual_campus.entity.ClubMembership;
import com.kaung.uit_virtual_campus.entity.ClubMembershipStatus;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClubRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMembershipRepository clubMembershipRepository;

    @Autowired
    private ClubGalleryRepository clubGalleryRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    private User leader;
    private User member;
    private Club club;

    @BeforeEach
    void setUp() {
        Role studentRole = entityManager.persist(new Role((short) 1, "STUDENT"));
        Role leaderRole = entityManager.persist(new Role((short) 2, "CLUB_LEADER"));
        leader = entityManager.persist(new User(UUID.randomUUID(), "leader@example.com", "Leader", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(studentRole, leaderRole)));
        member = entityManager.persist(new User(UUID.randomUUID(), "member@example.com", "Member", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(studentRole)));
        StudentProfile leaderProfile = entityManager.persist(new StudentProfile(leader, "S001", "CS", (short) 1));
        StudentProfile memberProfile = entityManager.persist(new StudentProfile(member, "S002", "CS", (short) 1));
        club = entityManager.persist(new Club(leader, "Programming Club", "Desc", "club@example.com", null, null));
        entityManager.persist(new ClubMembership(club, leaderProfile));
        entityManager.persist(new ClubMembership(club, memberProfile));
        entityManager.persist(new ClubGallery(club, "https://example.com/image.jpg", "Caption"));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Finds club with leader")
    void findsClubWithLeader() {
        Optional<Club> found = clubRepository.findByIdWithLeader(club.getClubId());

        assertThat(found).isPresent();
        assertThat(found.get().getLeader().getEmail()).isEqualTo("leader@example.com");
    }

    @Test
    @DisplayName("Finds active members with student details")
    void findsActiveMembers() {
        List<ClubMembership> memberships = clubMembershipRepository.findByClubIdAndStatusWithDetails(
                club.getClubId(),
                ClubMembershipStatus.ACTIVE
        );

        assertThat(memberships).hasSize(2);
        assertThat(memberships).extracting(m -> m.getStudent().getUser().getFullName())
                .contains("Leader", "Member");
    }

    @Test
    @DisplayName("Counts active members by club ids")
    void countsActiveMembersByClubIds() {
        List<ClubMembershipRepository.ClubMemberCount> counts = clubMembershipRepository.countByClubIdsAndStatus(
                List.of(club.getClubId()),
                ClubMembershipStatus.ACTIVE
        );

        assertThat(counts).hasSize(1);
        assertThat(counts.get(0).getClubId()).isEqualTo(club.getClubId());
        assertThat(counts.get(0).getActiveMemberCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Finds gallery items for club")
    void findsGallery() {
        List<ClubGallery> gallery = clubGalleryRepository.findByClubIdWithClub(club.getClubId());

        assertThat(gallery).hasSize(1);
        assertThat(gallery.get(0).getImageUrl()).isEqualTo("https://example.com/image.jpg");
    }

    @Test
    @DisplayName("Finds student profile with user")
    void findsStudentProfile() {
        Optional<StudentProfile> found = studentProfileRepository.findWithUserByStudentId(member.getUserId());

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("member@example.com");
    }
}
