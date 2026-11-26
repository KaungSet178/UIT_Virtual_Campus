package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.CurrentUserResponse;
import com.kaung.uit_virtual_campus.dto.StudentOnboardingRequest;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.AcademicGroupRepository;
import com.kaung.uit_virtual_campus.repository.RoleRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentOnboardingServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private CurrentUserProfileService currentUserProfileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private AcademicGroupRepository academicGroupRepository;

    @Mock
    private StudentElectiveService studentElectiveService;

    private StudentOnboardingService service;
    private UUID userId;
    private Role studentRole;
    private User user;

    @BeforeEach
    void setUp() {
        service = new StudentOnboardingService(
                currentUserService,
                currentUserProfileService,
                userRepository,
                roleRepository,
                studentProfileRepository,
                academicGroupRepository,
                studentElectiveService
        );
        userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        studentRole = new Role((short) 1, "STUDENT");
        user = new User(
                userId,
                "student@uit.example",
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of()
        );
    }

    @Test
    @DisplayName("Authenticated student onboarding creates section profile and assigns STUDENT role")
    void createsSectionProfileAndAssignsStudentRole() {
        AcademicGroup section = new AcademicGroup(1L, (short) 1, AcademicGroupType.SECTION, "A", null, null);
        StudentOnboardingRequest request = new StudentOnboardingRequest(" Student User ", " UIT-001 ", 1L, " 091234 ");
        CurrentUserResponse expected = response(section, null, null, (short) 1);
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findWithRolesByUserId(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleNameIgnoreCase("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByStudentNumber("UIT-001")).thenReturn(Optional.empty());
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(section));
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(expected);

        CurrentUserResponse response = service.completeStudentOnboarding(request);

        assertThat(response).isSameAs(expected);
        assertThat(user.getRoles()).containsExactly(studentRole);
        assertThat(user.getFullName()).isEqualTo("Student User");
        assertThat(user.getPhone()).isEqualTo("091234");
        ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(profileCaptor.capture());
        StudentProfile savedProfile = profileCaptor.getValue();
        assertThat(savedProfile.getStudentNumber()).isEqualTo("UIT-001");
        assertThat(savedProfile.getYear()).isEqualTo((short) 1);
        assertThat(savedProfile.getDepartment()).isNull();
        assertThat(savedProfile.getProgramme()).isNull();
        assertThat(savedProfile.getAcademicGroup()).isEqualTo(section);
    }

    @Test
    @DisplayName("Year 3 department onboarding creates public user without requiring a major")
    void createsMissingPublicUser() {
        AcademicGroup department = new AcademicGroup(5L, (short) 3, AcademicGroupType.DEPARTMENT, null, "CS", null);
        StudentOnboardingRequest request = new StudentOnboardingRequest("New Student", "UIT-002", 5L, null);
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(currentUserService.getCurrentUserEmail()).thenReturn(Optional.of("new.student@uit.example"));
        when(userRepository.findWithRolesByUserId(userId)).thenReturn(Optional.empty());
        when(roleRepository.findByRoleNameIgnoreCase("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByStudentNumber("UIT-002")).thenReturn(Optional.empty());
        when(academicGroupRepository.findById(5L)).thenReturn(Optional.of(department));
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(response(department, "CS", null, (short) 3));

        service.completeStudentOnboarding(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("new.student@uit.example");
        assertThat(userCaptor.getValue().getRoles()).containsExactly(studentRole);
        ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getDepartment()).isEqualTo("CS");
        assertThat(profileCaptor.getValue().getProgramme()).isNull();
        assertThat(profileCaptor.getValue().getAcademicGroup()).isEqualTo(department);
    }

    @Test
    @DisplayName("Year 3 CT department onboarding maps department and leaves programme unset")
    void departmentGroupMapsDepartmentWithoutProgramme() {
        AcademicGroup department = new AcademicGroup(6L, (short) 3, AcademicGroupType.DEPARTMENT, null, "CT", null);
        StudentOnboardingRequest request = new StudentOnboardingRequest("Student User", "UIT-008", 6L, null);
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findWithRolesByUserId(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleNameIgnoreCase("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByStudentNumber("UIT-008")).thenReturn(Optional.empty());
        when(academicGroupRepository.findById(6L)).thenReturn(Optional.of(department));
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(response(department, "CT", null, (short) 3));

        service.completeStudentOnboarding(request);

        ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getYear()).isEqualTo((short) 3);
        assertThat(profileCaptor.getValue().getDepartment()).isEqualTo("CT");
        assertThat(profileCaptor.getValue().getProgramme()).isNull();
    }

    @Test
    @DisplayName("Major academic group maps year department and programme")
    void majorGroupMapsDepartmentAndProgramme() {
        AcademicGroup major = new AcademicGroup(5L, (short) 4, AcademicGroupType.MAJOR, null, "CT", "Cyber Security");
        StudentOnboardingRequest request = new StudentOnboardingRequest("Student User", "UIT-003", 5L, null);
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findWithRolesByUserId(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleNameIgnoreCase("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByStudentNumber("UIT-003")).thenReturn(Optional.empty());
        when(academicGroupRepository.findById(5L)).thenReturn(Optional.of(major));
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(response(major, "CT", "Cyber Security", (short) 4));

        service.completeStudentOnboarding(request);

        ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getYear()).isEqualTo((short) 4);
        assertThat(profileCaptor.getValue().getDepartment()).isEqualTo("CT");
        assertThat(profileCaptor.getValue().getProgramme()).isEqualTo("Cyber Security");
    }

    @Test
    @DisplayName("Retry returns existing profile and does not create duplicate StudentProfile")
    void retryReturnsExistingProfile() {
        AcademicGroup section = new AcademicGroup(1L, (short) 1, AcademicGroupType.SECTION, "A", null, null);
        StudentProfile existingProfile = new StudentProfile(user, "UIT-001", null, null, (short) 1, section);
        CurrentUserResponse expected = response(section, null, null, (short) 1);
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findWithRolesByUserId(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleNameIgnoreCase("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.of(existingProfile));
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(expected);

        CurrentUserResponse response = service.completeStudentOnboarding(
                new StudentOnboardingRequest("Student User", "UIT-001", 1L, null)
        );

        assertThat(response).isSameAs(expected);
        verify(studentProfileRepository, never()).save(any(StudentProfile.class));
        verify(academicGroupRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Existing STUDENT role is not duplicated")
    void studentRoleIsNotDuplicated() {
        User existingStudent = new User(
                userId,
                "student@uit.example",
                "Student User",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(studentRole)
        );
        AcademicGroup section = new AcademicGroup(1L, (short) 1, AcademicGroupType.SECTION, "A", null, null);
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findWithRolesByUserId(userId)).thenReturn(Optional.of(existingStudent));
        when(roleRepository.findByRoleNameIgnoreCase("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByStudentNumber("UIT-004")).thenReturn(Optional.empty());
        when(academicGroupRepository.findById(1L)).thenReturn(Optional.of(section));
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(response(section, null, null, (short) 1));

        service.completeStudentOnboarding(new StudentOnboardingRequest("Student User", "UIT-004", 1L, null));

        assertThat(existingStudent.getRoles()).containsExactly(studentRole);
    }

    @Test
    @DisplayName("Onboarding enrolls selected elective course offering with the new student profile")
    void onboardingEnrollsSelectedElective() {
        AcademicGroup section = new AcademicGroup(2L, (short) 2, AcademicGroupType.SECTION, "A", null, null);
        StudentOnboardingRequest request = new StudentOnboardingRequest(
                "Student User",
                "UIT-005",
                2L,
                null,
                10L
        );
        CurrentUserResponse expected = response(section, null, null, (short) 2);
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findWithRolesByUserId(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleNameIgnoreCase("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByStudentNumber("UIT-005")).thenReturn(Optional.empty());
        when(academicGroupRepository.findById(2L)).thenReturn(Optional.of(section));
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(expected);

        CurrentUserResponse response = service.completeStudentOnboarding(request);

        assertThat(response).isSameAs(expected);
        ArgumentCaptor<StudentProfile> profileCaptor = ArgumentCaptor.forClass(StudentProfile.class);
        verify(studentProfileRepository).save(profileCaptor.capture());
        verify(studentElectiveService).enrollForOnboarding(profileCaptor.getValue(), 10L);
    }

    @Test
    @DisplayName("Duplicate student number returns conflict")
    void duplicateStudentNumberReturnsConflict() {
        User otherUser = new User(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "other@uit.example",
                "Other",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(studentRole)
        );
        AcademicGroup section = new AcademicGroup(1L, (short) 1, AcademicGroupType.SECTION, "A", null, null);
        StudentProfile otherProfile = new StudentProfile(otherUser, "UIT-001", null, null, (short) 1, section);
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findWithRolesByUserId(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleNameIgnoreCase("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByStudentNumber("UIT-001")).thenReturn(Optional.of(otherProfile));

        assertThatThrownBy(() -> service.completeStudentOnboarding(
                new StudentOnboardingRequest("Student User", "UIT-001", 1L, null)
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Student number is already registered.");
    }

    @Test
    @DisplayName("Nonexistent academic group returns not found")
    void nonexistentAcademicGroupReturnsNotFound() {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(userRepository.findWithRolesByUserId(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByRoleNameIgnoreCase("STUDENT")).thenReturn(Optional.of(studentRole));
        when(studentProfileRepository.findWithUserByStudentId(userId)).thenReturn(Optional.empty());
        when(studentProfileRepository.findByStudentNumber("UIT-001")).thenReturn(Optional.empty());
        when(academicGroupRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeStudentOnboarding(
                new StudentOnboardingRequest("Student User", "UIT-001", 999L, null)
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Academic group not found with id: 999");
    }

    private CurrentUserResponse response(AcademicGroup group, String department, String programme, Short year) {
        return new CurrentUserResponse(
                userId,
                "student@uit.example",
                "Student User",
                null,
                null,
                List.of("STUDENT"),
                new com.kaung.uit_virtual_campus.dto.StudentProfileResponse(
                        "UIT-001",
                        department,
                        programme,
                        year,
                        new com.kaung.uit_virtual_campus.dto.AcademicGroupResponse(
                                group.getGroupId(),
                                group.getYear(),
                                group.getGroupType(),
                                group.getSectionName(),
                                group.getDepartment(),
                                group.getMajor()
                        )
                ),
                null
        );
    }
}
