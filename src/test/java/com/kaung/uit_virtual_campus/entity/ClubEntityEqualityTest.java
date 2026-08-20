package com.kaung.uit_virtual_campus.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClubEntityEqualityTest {

    @Test
    @DisplayName("Unsaved clubs are not equal just because ids are null")
    void unsavedClubsAreNotEqual() {
        User leader = user("leader@example.com");

        Club first = new Club(leader, "Programming Club", null, null, null, null);
        Club second = new Club(leader, "Robotics Club", null, null, null, null);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Unsaved memberships are not equal just because ids are null")
    void unsavedMembershipsAreNotEqual() {
        User leader = user("leader@example.com");
        Club club = new Club(leader, "Programming Club", null, null, null, null);
        StudentProfile firstStudent = new StudentProfile(user("first@example.com"), "S001", "CS", (short) 1);
        StudentProfile secondStudent = new StudentProfile(user("second@example.com"), "S002", "CS", (short) 1);

        ClubMembership first = new ClubMembership(club, firstStudent);
        ClubMembership second = new ClubMembership(club, secondStudent);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Unsaved gallery items are not equal just because ids are null")
    void unsavedGalleryItemsAreNotEqual() {
        User leader = user("leader@example.com");
        Club club = new Club(leader, "Programming Club", null, null, null, null);

        ClubGallery first = new ClubGallery(club, "https://example.com/one.jpg", null);
        ClubGallery second = new ClubGallery(club, "https://example.com/two.jpg", null);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Student profiles compare by assigned student id")
    void studentProfilesCompareByAssignedStudentId() {
        User user = user("student@example.com");

        StudentProfile first = new StudentProfile(user, "S001", "CS", (short) 1);
        StudentProfile second = new StudentProfile(user, "S001", "CS", (short) 1);

        assertThat(first).isEqualTo(second);
    }

    private User user(String email) {
        return new User(UUID.randomUUID(), email, email, null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(new Role((short) 1, "STUDENT")));
    }
}
