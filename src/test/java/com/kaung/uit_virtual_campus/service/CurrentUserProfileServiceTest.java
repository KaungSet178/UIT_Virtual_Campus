package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.CurrentUserResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.StaffProfile;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.repository.StaffProfileRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserProfileServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private StaffProfileRepository staffProfileRepository;

    @Test
    @DisplayName("Current user profile includes safe user fields and optional student profile")
    void currentUserProfileIncludesStudentProfile() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "student@uit.example",
                "Student User",
                "https://example.test/profile.png",
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(new Role((short) 2, "STUDENT"))
        );
        AcademicGroup academicGroup = new AcademicGroup(5L, (short) 4, AcademicGroupType.MAJOR, null, "CS", "SE");
        StudentProfile studentProfile = new StudentProfile(
                user,
                "UIT-001",
                "CS",
                "Software Engineering",
                (short) 4,
                academicGroup
        );
        user.completeProfile("Student User", "+95 9 123 456 789");
        CurrentUserProfileService service = new CurrentUserProfileService(
                currentUserService,
                studentProfileRepository,
                staffProfileRepository
        );
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.of(studentProfile));
        when(staffProfileRepository.findWithUserByStaffId(userId)).thenReturn(Optional.empty());

        CurrentUserResponse response = service.getCurrentUserProfile();

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.profileImage()).isEqualTo("https://example.test/profile.png");
        assertThat(response.phone()).isEqualTo("+95 9 123 456 789");
        assertThat(response.roles()).containsExactly("STUDENT");
        assertThat(response.studentProfile().studentNumber()).isEqualTo("UIT-001");
        assertThat(response.studentProfile().department()).isEqualTo("CS");
        assertThat(response.studentProfile().programme()).isEqualTo("Software Engineering");
        assertThat(response.studentProfile().year()).isEqualTo((short) 4);
        assertThat(response.studentProfile().academicGroup().groupId()).isEqualTo(5L);
        assertThat(response.studentProfile().academicGroup().groupType()).isEqualTo(AcademicGroupType.MAJOR);
        assertThat(response.studentProfile().academicGroup().department()).isEqualTo("CS");
        assertThat(response.studentProfile().academicGroup().major()).isEqualTo("SE");
    }

    @Test
    @DisplayName("Current user profile returns null academic group when unassigned")
    void currentUserProfileAllowsNullAcademicGroup() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "student@uit.example",
                "Student User",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(new Role((short) 2, "STUDENT"))
        );
        StudentProfile studentProfile = new StudentProfile(user, "UIT-001", "CS", "Computer Science", (short) 3, null);
        CurrentUserProfileService service = new CurrentUserProfileService(
                currentUserService,
                studentProfileRepository,
                staffProfileRepository
        );
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.of(studentProfile));
        when(staffProfileRepository.findWithUserByStaffId(userId)).thenReturn(Optional.empty());

        CurrentUserResponse response = service.getCurrentUserProfile();

        assertThat(response.studentProfile().academicGroup()).isNull();
    }

    @Test
    @DisplayName("Current user profile includes optional staff profile")
    void currentUserProfileIncludesStaffProfile() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "admin@uit.example",
                "Admin User",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(new Role((short) 1, "ADMIN"))
        );
        StaffProfile staffProfile = new StaffProfile(
                user,
                "UIT-STF-001",
                "Student Affairs",
                "Administrator"
        );
        CurrentUserProfileService service = new CurrentUserProfileService(
                currentUserService,
                studentProfileRepository,
                staffProfileRepository
        );
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.empty());
        when(staffProfileRepository.findWithUserByStaffId(userId)).thenReturn(Optional.of(staffProfile));

        CurrentUserResponse response = service.getCurrentUserProfile();

        assertThat(response.roles()).containsExactly("ADMIN");
        assertThat(response.studentProfile()).isNull();
        assertThat(response.staffProfile().employeeNumber()).isEqualTo("UIT-STF-001");
        assertThat(response.staffProfile().department()).isEqualTo("Student Affairs");
        assertThat(response.staffProfile().position()).isEqualTo("Administrator");
    }

    @Test
    @DisplayName("Current user profile allows missing staff profile")
    void currentUserProfileAllowsNullStaffProfile() {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "admin@uit.example",
                "Admin User",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(new Role((short) 1, "ADMIN"))
        );
        CurrentUserProfileService service = new CurrentUserProfileService(
                currentUserService,
                studentProfileRepository,
                staffProfileRepository
        );
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.empty());
        when(staffProfileRepository.findWithUserByStaffId(userId)).thenReturn(Optional.empty());

        CurrentUserResponse response = service.getCurrentUserProfile();

        assertThat(response.staffProfile()).isNull();
    }
}
