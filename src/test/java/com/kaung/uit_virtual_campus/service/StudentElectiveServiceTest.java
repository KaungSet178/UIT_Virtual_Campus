package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.dto.ElectiveCourseOfferingResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.CourseOfferingType;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.StudentElectiveEnrollment;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.Subject;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.CourseOfferingRepository;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import com.kaung.uit_virtual_campus.repository.SemesterRepository;
import com.kaung.uit_virtual_campus.repository.StudentElectiveEnrollmentRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentElectiveServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private StudentElectiveEnrollmentRepository enrollmentRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private AcademicGroupService academicGroupService;

    private StudentElectiveService electiveService;
    private User user;
    private StudentProfile studentProfile;
    private Semester activeSemester;
    private AcademicGroup academicGroup;
    private CourseOffering electiveOffering;
    private CourseOffering mandatoryOffering;
    private Schedule electiveSchedule;

    @BeforeEach
    void setUp() {
        electiveService = new StudentElectiveService(
                currentUserService,
                studentProfileRepository,
                semesterRepository,
                courseOfferingRepository,
                enrollmentRepository,
                scheduleRepository,
                academicGroupService,
                Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC)
        );

        user = new User(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "student@uit.example",
                "Student User",
                null,
                null,
                null,
                Set.of()
        );
        activeSemester = new Semester(1L, "2026 Semester 2", LocalDate.of(2026, 6, 15), LocalDate.of(2026, 10, 30));
        academicGroup = new AcademicGroup(2L, (short) 2, AcademicGroupType.SECTION, "A", null, null);
        studentProfile = new StudentProfile(user, "UIT-001", null, null, (short) 2, academicGroup);
        Subject electiveSubject = new Subject(3L, "CST-4105", "Java Keystone", null);
        Subject mandatorySubject = new Subject(4L, "CS201", "Mandatory Subject", null);
        electiveOffering = new CourseOffering(5L, activeSemester, academicGroup, electiveSubject, CourseOfferingType.ELECTIVE);
        mandatoryOffering = new CourseOffering(6L, activeSemester, academicGroup, mandatorySubject, CourseOfferingType.MANDATORY);
        Building building = new Building(7L, "Building 1", "Main");
        Floor floor = new Floor(8L, building, 1);
        Room room = new Room(9L, floor, "111", "Room 111", "CLASSROOM", "Room", "room_111");
        electiveSchedule = new Schedule(10L, electiveOffering, room, (short) 2, LocalTime.of(13, 0), LocalTime.of(14, 0));
        ReflectionTestUtils.setField(electiveOffering, "schedules", List.of(electiveSchedule));

        lenient().when(currentUserService.getCurrentUser()).thenReturn(user);
        lenient().when(studentProfileRepository.findWithUserByStudentId(user.getUserId()))
                .thenReturn(Optional.of(studentProfile));
    }

    @Test
    @DisplayName("Student can list available electives with enrollment marker")
    void studentCanListAvailableElectives() {
        when(semesterRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1)
        )).thenReturn(List.of(activeSemester));
        when(enrollmentRepository.findCurrentEnrolledOfferingIds(studentProfile.getStudentId(), 1L))
                .thenReturn(Set.of(5L));
        when(courseOfferingRepository.findAvailableElectives(1L, 2L)).thenReturn(List.of(electiveOffering));
        when(academicGroupService.toResponse(academicGroup))
                .thenReturn(new AcademicGroupResponse(2L, (short) 2, AcademicGroupType.SECTION, "A", null, null));

        List<ElectiveCourseOfferingResponse> response = electiveService.getAvailableElectives();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).subjectCode()).isEqualTo("CST-4105");
        assertThat(response.get(0).enrolled()).isTrue();
    }

    @Test
    @DisplayName("Signup availability for Year 3 CS returns only CS department electives")
    void signupAvailabilityReturnsYearThreeCsDepartmentElectives() {
        AcademicGroup csDepartment = new AcademicGroup(31L, (short) 3, AcademicGroupType.DEPARTMENT, null, "CS", null);
        CourseOffering csElective = new CourseOffering(
                41L,
                activeSemester,
                csDepartment,
                new Subject(51L, "CST-3101", "CS Elective", null),
                CourseOfferingType.ELECTIVE
        );
        when(semesterRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1)
        )).thenReturn(List.of(activeSemester));
        when(courseOfferingRepository.findAvailableElectives(1L, 31L)).thenReturn(List.of(csElective));
        when(academicGroupService.toResponse(csDepartment))
                .thenReturn(new AcademicGroupResponse(31L, (short) 3, AcademicGroupType.DEPARTMENT, null, "CS", null));

        List<ElectiveCourseOfferingResponse> response = electiveService.getAvailableElectives(31L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).academicGroup().groupType()).isEqualTo(AcademicGroupType.DEPARTMENT);
        assertThat(response.get(0).academicGroup().department()).isEqualTo("CS");
        assertThat(response.get(0).academicGroup().major()).isNull();
        verify(courseOfferingRepository).findAvailableElectives(1L, 31L);
    }

    @Test
    @DisplayName("Signup availability for Year 3 CT returns only CT department electives")
    void signupAvailabilityReturnsYearThreeCtDepartmentElectives() {
        AcademicGroup ctDepartment = new AcademicGroup(32L, (short) 3, AcademicGroupType.DEPARTMENT, null, "CT", null);
        CourseOffering ctElective = new CourseOffering(
                42L,
                activeSemester,
                ctDepartment,
                new Subject(52L, "CT-3101", "CT Elective", null),
                CourseOfferingType.ELECTIVE
        );
        when(semesterRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1)
        )).thenReturn(List.of(activeSemester));
        when(courseOfferingRepository.findAvailableElectives(1L, 32L)).thenReturn(List.of(ctElective));
        when(academicGroupService.toResponse(ctDepartment))
                .thenReturn(new AcademicGroupResponse(32L, (short) 3, AcademicGroupType.DEPARTMENT, null, "CT", null));

        List<ElectiveCourseOfferingResponse> response = electiveService.getAvailableElectives(32L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).academicGroup().groupType()).isEqualTo(AcademicGroupType.DEPARTMENT);
        assertThat(response.get(0).academicGroup().department()).isEqualTo("CT");
        assertThat(response.get(0).academicGroup().major()).isNull();
        verify(courseOfferingRepository).findAvailableElectives(1L, 32L);
    }

    @Test
    @DisplayName("Student can enroll in a valid elective")
    void studentCanEnrollInValidElective() {
        stubActiveSemester();
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(5L)).thenReturn(Optional.of(electiveOffering));
        when(enrollmentRepository.existsByStudentProfileStudentIdAndCourseOfferingOfferingId(studentProfile.getStudentId(), 5L))
                .thenReturn(false);
        when(enrollmentRepository.save(any(StudentElectiveEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(academicGroupService.toResponse(academicGroup))
                .thenReturn(new AcademicGroupResponse(2L, (short) 2, AcademicGroupType.SECTION, "A", null, null));

        assertThat(electiveService.enroll(5L).courseOffering().subjectCode()).isEqualTo("CST-4105");
        verify(enrollmentRepository).save(any(StudentElectiveEnrollment.class));
    }

    @Test
    @DisplayName("Cannot enroll in a mandatory course offering")
    void cannotEnrollInMandatoryCourseOffering() {
        stubActiveSemester();
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(6L)).thenReturn(Optional.of(mandatoryOffering));

        assertThatThrownBy(() -> electiveService.enroll(6L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot enroll in a mandatory course offering.");
    }

    @Test
    @DisplayName("Cannot enroll twice in the same elective")
    void cannotEnrollTwice() {
        stubActiveSemester();
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(5L)).thenReturn(Optional.of(electiveOffering));
        when(enrollmentRepository.existsByStudentProfileStudentIdAndCourseOfferingOfferingId(studentProfile.getStudentId(), 5L))
                .thenReturn(true);

        assertThatThrownBy(() -> electiveService.enroll(5L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Student is already enrolled in this elective.");
    }

    @Test
    @DisplayName("Cannot enroll in an elective from another academic group")
    void cannotEnrollInOtherAcademicGroupElective() {
        stubActiveSemester();
        AcademicGroup otherGroup = new AcademicGroup(99L, (short) 2, AcademicGroupType.SECTION, "B", null, null);
        CourseOffering otherGroupOffering = new CourseOffering(
                7L,
                activeSemester,
                otherGroup,
                new Subject(8L, "CST-4408", "Cybersecurity", null),
                CourseOfferingType.ELECTIVE
        );
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(7L)).thenReturn(Optional.of(otherGroupOffering));

        assertThatThrownBy(() -> electiveService.enroll(7L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Elective course offering is not available to the student's academic group.");
    }

    @Test
    @DisplayName("Cannot enroll in an elective from the wrong semester")
    void cannotEnrollInWrongSemesterElective() {
        stubActiveSemester();
        Semester oldSemester = new Semester(99L, "Old Semester", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 1));
        CourseOffering oldOffering = new CourseOffering(
                7L,
                oldSemester,
                academicGroup,
                new Subject(8L, "CST-4408", "Cybersecurity", null),
                CourseOfferingType.ELECTIVE
        );
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(7L)).thenReturn(Optional.of(oldOffering));

        assertThatThrownBy(() -> electiveService.enroll(7L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Elective course offering is not in the active semester.");
    }

    @Test
    @DisplayName("Rejects elective when it conflicts with mandatory timetable")
    void rejectsMandatoryTimetableConflict() {
        stubActiveSemester();
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(5L)).thenReturn(Optional.of(electiveOffering));
        when(enrollmentRepository.existsByStudentProfileStudentIdAndCourseOfferingOfferingId(studentProfile.getStudentId(), 5L))
                .thenReturn(false);
        when(scheduleRepository.countMandatoryTimetableOverlaps(
                1L,
                2L,
                (short) 2,
                LocalTime.of(13, 0),
                LocalTime.of(14, 0)
        )).thenReturn(1L);

        assertThatThrownBy(() -> electiveService.enroll(5L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Elective schedule conflicts with the student's existing timetable.");
    }

    @Test
    @DisplayName("Rejects elective when it conflicts with another selected elective")
    void rejectsSelectedElectiveConflict() {
        stubActiveSemester();
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(5L)).thenReturn(Optional.of(electiveOffering));
        when(enrollmentRepository.existsByStudentProfileStudentIdAndCourseOfferingOfferingId(studentProfile.getStudentId(), 5L))
                .thenReturn(false);
        when(scheduleRepository.countStudentElectiveOverlaps(
                studentProfile.getStudentId(),
                1L,
                5L,
                (short) 2,
                LocalTime.of(13, 0),
                LocalTime.of(14, 0)
        )).thenReturn(1L);

        assertThatThrownBy(() -> electiveService.enroll(5L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Elective schedule conflicts with the student's existing timetable.");
    }

    @Test
    @DisplayName("Back-to-back elective schedules are allowed")
    void backToBackElectiveSchedulesAreAllowed() {
        stubActiveSemester();
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(5L)).thenReturn(Optional.of(electiveOffering));
        when(enrollmentRepository.existsByStudentProfileStudentIdAndCourseOfferingOfferingId(studentProfile.getStudentId(), 5L))
                .thenReturn(false);
        when(enrollmentRepository.save(any(StudentElectiveEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(academicGroupService.toResponse(academicGroup))
                .thenReturn(new AcademicGroupResponse(2L, (short) 2, AcademicGroupType.SECTION, "A", null, null));

        electiveService.enroll(5L);

        verify(scheduleRepository).countMandatoryTimetableOverlaps(
                1L,
                2L,
                (short) 2,
                LocalTime.of(13, 0),
                LocalTime.of(14, 0)
        );
        verify(scheduleRepository).countStudentElectiveOverlaps(
                studentProfile.getStudentId(),
                1L,
                5L,
                (short) 2,
                LocalTime.of(13, 0),
                LocalTime.of(14, 0)
        );
    }

    @Test
    @DisplayName("Missing course offering returns not found")
    void missingCourseOfferingReturnsNotFound() {
        stubActiveSemester();
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> electiveService.enroll(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Course offering not found with id: 99");
    }

    @Test
    @DisplayName("Signup onboarding does not require an elective when no active elective exists for the academic group")
    void onboardingDoesNotRequireElectiveWhenNoAvailableElectivesExist() {
        stubActiveSemester();
        when(courseOfferingRepository.findAvailableElectives(1L, 2L)).thenReturn(List.of());

        electiveService.enrollForOnboarding(studentProfile, null);

        verify(enrollmentRepository, never()).save(any(StudentElectiveEnrollment.class));
    }

    @Test
    @DisplayName("Signup onboarding rejects missing elective when the academic group has electives")
    void onboardingRejectsMissingElectiveWhenAvailableElectivesExist() {
        stubActiveSemester();
        when(courseOfferingRepository.findAvailableElectives(1L, 2L)).thenReturn(List.of(electiveOffering));

        assertThatThrownBy(() -> electiveService.enrollForOnboarding(studentProfile, null))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Please choose one elective subject before continuing.");
    }

    @Test
    @DisplayName("Signup onboarding accepts exactly one valid elective")
    void onboardingAcceptsExactlyOneValidElective() {
        stubActiveSemester();
        when(courseOfferingRepository.findAvailableElectives(1L, 2L)).thenReturn(List.of(electiveOffering));
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(5L)).thenReturn(Optional.of(electiveOffering));
        when(enrollmentRepository.existsByStudentProfileStudentIdAndCourseOfferingOfferingId(studentProfile.getStudentId(), 5L))
                .thenReturn(false);
        when(enrollmentRepository.save(any(StudentElectiveEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        electiveService.enrollForOnboarding(studentProfile, 5L);

        verify(enrollmentRepository).save(any(StudentElectiveEnrollment.class));
    }

    @Test
    @DisplayName("Signup onboarding rejects mandatory offering as elective")
    void onboardingRejectsMandatoryOfferingAsElective() {
        stubActiveSemester();
        when(courseOfferingRepository.findAvailableElectives(1L, 2L)).thenReturn(List.of(electiveOffering));
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(6L)).thenReturn(Optional.of(mandatoryOffering));

        assertThatThrownBy(() -> electiveService.enrollForOnboarding(studentProfile, 6L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot enroll in a mandatory course offering.");
    }

    @Test
    @DisplayName("Signup onboarding rejects elective from another academic group")
    void onboardingRejectsElectiveFromAnotherAcademicGroup() {
        stubActiveSemester();
        AcademicGroup otherGroup = new AcademicGroup(99L, (short) 2, AcademicGroupType.SECTION, "B", null, null);
        CourseOffering otherGroupOffering = new CourseOffering(
                7L,
                activeSemester,
                otherGroup,
                new Subject(8L, "CST-4408", "Cybersecurity", null),
                CourseOfferingType.ELECTIVE
        );
        when(courseOfferingRepository.findAvailableElectives(1L, 2L)).thenReturn(List.of(electiveOffering));
        when(courseOfferingRepository.findByIdWithDetailsAndSchedules(7L)).thenReturn(Optional.of(otherGroupOffering));

        assertThatThrownBy(() -> electiveService.enrollForOnboarding(studentProfile, 7L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Elective course offering is not available to the student's academic group.");
    }

    private void stubActiveSemester() {
        when(semesterRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1)
        )).thenReturn(List.of(activeSemester));
    }
}
