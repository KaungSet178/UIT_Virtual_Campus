package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ScheduleRequest;
import com.kaung.uit_virtual_campus.dto.ScheduleResponse;
import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.CourseOfferingType;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.Subject;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.CourseOfferingRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import com.kaung.uit_virtual_campus.repository.SemesterRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TimetableValidationService validationService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private AcademicGroupService academicGroupService;

    private ScheduleService scheduleService;
    private CourseOffering offering;
    private Room room;
    private Schedule schedule;
    private User user;
    private AcademicGroup academicGroup;
    private AcademicGroupResponse academicGroupResponse;
    private Semester semester;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                scheduleRepository,
                courseOfferingRepository,
                roomRepository,
                validationService,
                currentUserService,
                studentProfileRepository,
                semesterRepository,
                Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC),
                academicGroupService
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
        semester = new Semester(1L, "2026 Semester 2", LocalDate.of(2026, 6, 15), LocalDate.of(2026, 10, 30));
        academicGroup = new AcademicGroup(2L, (short) 3, AcademicGroupType.MAJOR, null, "CS", "SE");
        academicGroupResponse = new AcademicGroupResponse(2L, (short) 3, AcademicGroupType.MAJOR, null, "CS", "SE");
        Subject subject = new Subject(3L, "CS101", "Programming Fundamentals", "Intro");
        offering = new CourseOffering(4L, semester, academicGroup, subject);
        Building building = new Building(1L, "Building 1", "Main");
        Floor floor = new Floor(101L, building, 1);
        room = new Room(10L, floor, "111", "Room 111", "CLASSROOM", "Room", "room_111");
        schedule = new Schedule(20L, offering, room, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("Should create schedule after validation")
    void createSchedule() {
        ScheduleRequest request = new ScheduleRequest(4L, 10L, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(courseOfferingRepository.findByIdWithDetails(4L)).thenReturn(Optional.of(offering));
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));
        when(scheduleRepository.save(org.mockito.ArgumentMatchers.any(Schedule.class))).thenReturn(schedule);
        when(scheduleRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(schedule));
        when(academicGroupService.toResponse(academicGroup)).thenReturn(academicGroupResponse);

        ScheduleResponse response = scheduleService.createSchedule(request);

        assertThat(response.scheduleId()).isEqualTo(20L);
        assertThat(response.dayName()).isEqualTo("MONDAY");
        verify(validationService).validateScheduleTime((short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
        verify(validationService).validateNoRoomOverlap(10L, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0), 1L, 3L, 2L, null);
        verify(roomRepository).findByIdForUpdate(10L);
    }

    @Test
    @DisplayName("Should exclude current schedule during update overlap validation")
    void updateScheduleExcludesCurrentSchedule() {
        ScheduleRequest request = new ScheduleRequest(4L, 10L, (short) 2, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Schedule updated = new Schedule(20L, offering, room, (short) 2, LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(scheduleRepository.findById(20L)).thenReturn(Optional.of(schedule));
        when(courseOfferingRepository.findByIdWithDetails(4L)).thenReturn(Optional.of(offering));
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));
        when(scheduleRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(updated));
        when(academicGroupService.toResponse(academicGroup)).thenReturn(academicGroupResponse);

        ScheduleResponse response = scheduleService.updateSchedule(20L, request);

        assertThat(response.dayOfWeek()).isEqualTo((short) 2);
        verify(validationService).validateNoRoomOverlap(10L, (short) 2, LocalTime.of(10, 0), LocalTime.of(11, 0), 1L, 3L, 2L, 20L);
        verify(roomRepository).findByIdForUpdate(10L);
    }

    @Test
    @DisplayName("Current student timetable uses assigned academic group and active semester")
    void currentStudentTimetableUsesAssignedAcademicGroupAndActiveSemester() {
        StudentProfile studentProfile = new StudentProfile(user, "UIT-001", "CS", "Software Engineering", (short) 3, academicGroup);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(user.getUserId())).thenReturn(Optional.of(studentProfile));
        when(semesterRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1)
        )).thenReturn(List.of(semester));
        when(scheduleRepository.findPersonalTimetable(1L, 2L)).thenReturn(List.of(schedule));
        when(academicGroupService.toResponse(academicGroup)).thenReturn(academicGroupResponse);

        List<ScheduleResponse> response = scheduleService.getCurrentStudentTimetable();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).academicGroup().groupId()).isEqualTo(2L);
        assertThat(response.get(0).semesterId()).isEqualTo(1L);
        verify(scheduleRepository).findPersonalTimetable(1L, 2L);
    }

    @Test
    @DisplayName("Students in same academic group receive same mandatory schedules and their own electives")
    void currentStudentTimetableAddsOnlySelectedElectives() {
        User studentA = user;
        User studentB = new User(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "student-b@uit.example",
                "Student B",
                null,
                null,
                null,
                Set.of()
        );
        StudentProfile profileA = new StudentProfile(studentA, "UIT-001", "CS", "Software Engineering", (short) 3, academicGroup);
        StudentProfile profileB = new StudentProfile(studentB, "UIT-002", "CS", "Software Engineering", (short) 3, academicGroup);
        Subject electiveSubjectA = new Subject(30L, "CST-4105", "Java Keystone", null);
        Subject electiveSubjectB = new Subject(31L, "CST-4408", "Cybersecurity", null);
        CourseOffering electiveOfferingA = new CourseOffering(40L, semester, academicGroup, electiveSubjectA, CourseOfferingType.ELECTIVE);
        CourseOffering electiveOfferingB = new CourseOffering(41L, semester, academicGroup, electiveSubjectB, CourseOfferingType.ELECTIVE);
        Schedule electiveA = new Schedule(50L, electiveOfferingA, room, (short) 2, LocalTime.of(13, 0), LocalTime.of(14, 0));
        Schedule electiveB = new Schedule(51L, electiveOfferingB, room, (short) 3, LocalTime.of(13, 0), LocalTime.of(14, 0));

        when(semesterRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1)
        )).thenReturn(List.of(semester));
        when(scheduleRepository.findPersonalTimetable(1L, 2L)).thenReturn(List.of(schedule));
        when(academicGroupService.toResponse(academicGroup)).thenReturn(academicGroupResponse);

        when(currentUserService.getCurrentUser()).thenReturn(studentA);
        when(studentProfileRepository.findWithUserByStudentId(studentA.getUserId())).thenReturn(Optional.of(profileA));
        when(scheduleRepository.findStudentElectiveSchedules(profileA.getStudentId(), 1L)).thenReturn(List.of(electiveA));

        List<ScheduleResponse> responseA = scheduleService.getCurrentStudentTimetable();

        assertThat(responseA).extracting(ScheduleResponse::subjectCode)
                .containsExactly("CS101", "CST-4105");
        assertThat(responseA).extracting(ScheduleResponse::subjectCode)
                .doesNotContain("CST-4408");
        assertThat(responseA.get(0).courseType()).isEqualTo(CourseOfferingType.MANDATORY);
        assertThat(responseA.get(1).courseType()).isEqualTo(CourseOfferingType.ELECTIVE);

        when(currentUserService.getCurrentUser()).thenReturn(studentB);
        when(studentProfileRepository.findWithUserByStudentId(studentB.getUserId())).thenReturn(Optional.of(profileB));
        when(scheduleRepository.findStudentElectiveSchedules(profileB.getStudentId(), 1L)).thenReturn(List.of(electiveB));

        List<ScheduleResponse> responseB = scheduleService.getCurrentStudentTimetable();

        assertThat(responseB).extracting(ScheduleResponse::subjectCode)
                .containsExactly("CS101", "CST-4408");
        assertThat(responseB).extracting(ScheduleResponse::subjectCode)
                .doesNotContain("CST-4105");
    }

    @Test
    @DisplayName("Current student timetable rejects missing student profile")
    void currentStudentTimetableRejectsMissingProfile() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(user.getUserId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleService.getCurrentStudentTimetable())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Student profile not found");
    }

    @Test
    @DisplayName("Current student timetable rejects unassigned academic group")
    void currentStudentTimetableRejectsNullAcademicGroup() {
        StudentProfile studentProfile = new StudentProfile(user, "UIT-001", "CS", "Software Engineering", (short) 3, null);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(user.getUserId())).thenReturn(Optional.of(studentProfile));

        assertThatThrownBy(() -> scheduleService.getCurrentStudentTimetable())
                .isInstanceOf(ConflictException.class)
                .hasMessage("Student academic group is not assigned.");
    }

    @Test
    @DisplayName("Current student timetable rejects mismatched profile and academic group year")
    void currentStudentTimetableRejectsMismatchedYear() {
        StudentProfile studentProfile = new StudentProfile(user, "UIT-001", "CS", "Software Engineering", (short) 2, academicGroup);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(user.getUserId())).thenReturn(Optional.of(studentProfile));

        assertThatThrownBy(() -> scheduleService.getCurrentStudentTimetable())
                .isInstanceOf(ConflictException.class)
                .hasMessage("Student profile year does not match assigned academic group year.");
    }

    @Test
    @DisplayName("Current student timetable rejects mismatched major department")
    void currentStudentTimetableRejectsMismatchedMajorDepartment() {
        StudentProfile studentProfile = new StudentProfile(user, "UIT-001", "CT", "Software Engineering", (short) 3, academicGroup);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(user.getUserId())).thenReturn(Optional.of(studentProfile));

        assertThatThrownBy(() -> scheduleService.getCurrentStudentTimetable())
                .isInstanceOf(ConflictException.class)
                .hasMessage("Student profile department does not match assigned academic group department.");
    }

    @Test
    @DisplayName("Current student timetable returns empty list when no semester is active")
    void currentStudentTimetableReturnsEmptyWithoutActiveSemester() {
        StudentProfile studentProfile = new StudentProfile(user, "UIT-001", "CS", "Software Engineering", (short) 3, academicGroup);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(user.getUserId())).thenReturn(Optional.of(studentProfile));
        when(semesterRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1)
        )).thenReturn(List.of());

        assertThat(scheduleService.getCurrentStudentTimetable()).isEmpty();
    }

    @Test
    @DisplayName("Current student timetable rejects multiple active semesters")
    void currentStudentTimetableRejectsMultipleActiveSemesters() {
        StudentProfile studentProfile = new StudentProfile(user, "UIT-001", "CS", "Software Engineering", (short) 3, academicGroup);
        Semester otherActive = new Semester(2L, "Overlap", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30));
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(studentProfileRepository.findWithUserByStudentId(user.getUserId())).thenReturn(Optional.of(studentProfile));
        when(semesterRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 1)
        )).thenReturn(List.of(otherActive, semester));

        assertThatThrownBy(() -> scheduleService.getCurrentStudentTimetable())
                .isInstanceOf(ConflictException.class)
                .hasMessage("Multiple active semesters are configured.");
    }
}
