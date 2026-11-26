package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.dto.ElectiveCourseOfferingResponse;
import com.kaung.uit_virtual_campus.dto.ElectiveEnrollmentResponse;
import com.kaung.uit_virtual_campus.dto.ScheduleResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.CourseOfferingType;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.StudentElectiveEnrollment;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.CourseOfferingRepository;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import com.kaung.uit_virtual_campus.repository.SemesterRepository;
import com.kaung.uit_virtual_campus.repository.StudentElectiveEnrollmentRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class StudentElectiveService {

    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;
    private final SemesterRepository semesterRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final StudentElectiveEnrollmentRepository enrollmentRepository;
    private final ScheduleRepository scheduleRepository;
    private final AcademicGroupService academicGroupService;
    private final Clock clock;

    public StudentElectiveService(
            CurrentUserService currentUserService,
            StudentProfileRepository studentProfileRepository,
            SemesterRepository semesterRepository,
            CourseOfferingRepository courseOfferingRepository,
            StudentElectiveEnrollmentRepository enrollmentRepository,
            ScheduleRepository scheduleRepository,
            AcademicGroupService academicGroupService,
            Clock clock
    ) {
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
        this.semesterRepository = semesterRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.scheduleRepository = scheduleRepository;
        this.academicGroupService = academicGroupService;
        this.clock = clock;
    }

    public List<ElectiveCourseOfferingResponse> getAvailableElectives() {
        StudentProfile studentProfile = getCurrentStudentProfile();
        Semester activeSemester = getActiveSemesterOrNull();
        if (activeSemester == null) {
            return List.of();
        }
        Set<Long> enrolledOfferingIds = enrollmentRepository.findCurrentEnrolledOfferingIds(
                studentProfile.getStudentId(),
                activeSemester.getSemesterId()
        );
        return courseOfferingRepository.findAvailableElectives(
                        activeSemester.getSemesterId(),
                        studentProfile.getAcademicGroup().getGroupId()
                )
                .stream()
                .map(offering -> toOfferingResponse(offering, enrolledOfferingIds.contains(offering.getOfferingId())))
                .toList();
    }

    public List<ElectiveCourseOfferingResponse> getAvailableElectives(Long academicGroupId) {
        Semester activeSemester = getActiveSemesterOrNull();
        if (activeSemester == null) {
            return List.of();
        }
        return courseOfferingRepository.findAvailableElectives(activeSemester.getSemesterId(), academicGroupId)
                .stream()
                .map(offering -> toOfferingResponse(offering, false))
                .toList();
    }

    public List<ElectiveEnrollmentResponse> getCurrentEnrollments() {
        StudentProfile studentProfile = getCurrentStudentProfile();
        Semester activeSemester = getActiveSemesterOrNull();
        if (activeSemester == null) {
            return List.of();
        }
        return enrollmentRepository.findCurrentEnrollments(
                        studentProfile.getStudentId(),
                        activeSemester.getSemesterId()
                )
                .stream()
                .map(this::toEnrollmentResponse)
                .toList();
    }

    @Transactional
    public ElectiveEnrollmentResponse enroll(Long courseOfferingId) {
        StudentProfile studentProfile = getCurrentStudentProfile();
        Semester activeSemester = getRequiredActiveSemester();
        CourseOffering courseOffering = courseOfferingRepository.findByIdWithDetailsAndSchedules(courseOfferingId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + courseOfferingId));

        StudentElectiveEnrollment saved = enroll(studentProfile, courseOffering, activeSemester);
        if (saved.getId() == null) {
            return toEnrollmentResponse(saved);
        }
        return enrollmentRepository.findCurrentEnrollments(studentProfile.getStudentId(), activeSemester.getSemesterId())
                .stream()
                .filter(enrollment -> enrollment.getId().equals(saved.getId()))
                .findFirst()
                .map(this::toEnrollmentResponse)
                .orElseGet(() -> toEnrollmentResponse(saved));
    }

    public void enrollForOnboarding(StudentProfile studentProfile, Long courseOfferingId) {
        Semester activeSemester = getActiveSemesterOrNull();
        if (activeSemester == null) {
            if (courseOfferingId != null) {
                throw new ConflictException("Elective course offering is not in the active semester.");
            }
            return;
        }

        List<CourseOffering> availableElectives = courseOfferingRepository.findAvailableElectives(
                activeSemester.getSemesterId(),
                studentProfile.getAcademicGroup().getGroupId()
        );
        if (availableElectives.isEmpty()) {
            if (courseOfferingId != null) {
                throw new ConflictException("Elective course offering is not available to the student's academic group.");
            }
            return;
        }
        if (courseOfferingId == null) {
            throw new ConflictException("Please choose one elective subject before continuing.");
        }

        CourseOffering courseOffering = courseOfferingRepository.findByIdWithDetailsAndSchedules(courseOfferingId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + courseOfferingId));
        enroll(studentProfile, courseOffering, activeSemester);
    }

    private StudentElectiveEnrollment enroll(
            StudentProfile studentProfile,
            CourseOffering courseOffering,
            Semester activeSemester
    ) {
        validateEnrollment(studentProfile, courseOffering, activeSemester);

        try {
            return enrollmentRepository.save(new StudentElectiveEnrollment(studentProfile, courseOffering));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Student is already enrolled in this elective.");
        }
    }

    @Transactional
    public void removeEnrollment(Long courseOfferingId) {
        StudentProfile studentProfile = getCurrentStudentProfile();
        StudentElectiveEnrollment enrollment = enrollmentRepository
                .findByStudentProfileStudentIdAndCourseOfferingOfferingId(
                        studentProfile.getStudentId(),
                        courseOfferingId
                )
                .orElseThrow(() -> new ResourceNotFoundException("Elective enrollment not found"));
        enrollmentRepository.delete(enrollment);
    }

    private void validateEnrollment(
            StudentProfile studentProfile,
            CourseOffering courseOffering,
            Semester activeSemester
    ) {
        if (courseOffering.getType() != CourseOfferingType.ELECTIVE) {
            throw new ConflictException("Cannot enroll in a mandatory course offering.");
        }
        if (!courseOffering.getSemester().getSemesterId().equals(activeSemester.getSemesterId())) {
            throw new ConflictException("Elective course offering is not in the active semester.");
        }
        if (!courseOffering.getAcademicGroup().getGroupId().equals(studentProfile.getAcademicGroup().getGroupId())) {
            throw new ConflictException("Elective course offering is not available to the student's academic group.");
        }
        if (enrollmentRepository.existsByStudentProfileStudentIdAndCourseOfferingOfferingId(
                studentProfile.getStudentId(),
                courseOffering.getOfferingId()
        )) {
            throw new ConflictException("Student is already enrolled in this elective.");
        }
        for (Schedule requested : courseOffering.getSchedules()) {
            long mandatoryConflicts = scheduleRepository.countMandatoryTimetableOverlaps(
                    activeSemester.getSemesterId(),
                    studentProfile.getAcademicGroup().getGroupId(),
                    requested.getDayOfWeek(),
                    requested.getStartTime(),
                    requested.getEndTime()
            );
            long electiveConflicts = scheduleRepository.countStudentElectiveOverlaps(
                    studentProfile.getStudentId(),
                    activeSemester.getSemesterId(),
                    courseOffering.getOfferingId(),
                    requested.getDayOfWeek(),
                    requested.getStartTime(),
                    requested.getEndTime()
            );
            if (mandatoryConflicts > 0 || electiveConflicts > 0) {
                throw new ConflictException("Elective schedule conflicts with the student's existing timetable.");
            }
        }
    }

    private StudentProfile getCurrentStudentProfile() {
        User user = currentUserService.getCurrentUser();
        StudentProfile studentProfile = studentProfileRepository.findWithUserByStudentId(user.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));
        AcademicGroup academicGroup = studentProfile.getAcademicGroup();
        if (academicGroup == null) {
            throw new ConflictException("Student academic group is not assigned.");
        }
        if (studentProfile.getYear() != null
                && academicGroup.getYear() != null
                && !studentProfile.getYear().equals(academicGroup.getYear())) {
            throw new ConflictException("Student profile year does not match assigned academic group year.");
        }
        if (academicGroup.getGroupType() != AcademicGroupType.SECTION
                && studentProfile.getDepartment() != null
                && academicGroup.getDepartment() != null
                && !studentProfile.getDepartment().equalsIgnoreCase(academicGroup.getDepartment())) {
            throw new ConflictException("Student profile department does not match assigned academic group department.");
        }
        return studentProfile;
    }

    private Semester getRequiredActiveSemester() {
        Semester semester = getActiveSemesterOrNull();
        if (semester == null) {
            throw new ConflictException("No active semester is configured.");
        }
        return semester;
    }

    private Semester getActiveSemesterOrNull() {
        LocalDate today = LocalDate.now(clock);
        List<Semester> activeSemesters = semesterRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                        today,
                        today
                );
        if (activeSemesters.isEmpty()) {
            return null;
        }
        if (activeSemesters.size() > 1) {
            throw new ConflictException("Multiple active semesters are configured.");
        }
        return activeSemesters.get(0);
    }

    private ElectiveEnrollmentResponse toEnrollmentResponse(StudentElectiveEnrollment enrollment) {
        return new ElectiveEnrollmentResponse(
                enrollment.getId(),
                enrollment.getCreatedAt(),
                toOfferingResponse(enrollment.getCourseOffering(), true)
        );
    }

    private ElectiveCourseOfferingResponse toOfferingResponse(CourseOffering offering, boolean enrolled) {
        return new ElectiveCourseOfferingResponse(
                offering.getOfferingId(),
                offering.getType(),
                offering.getSubject().getSubjectId(),
                offering.getSubject().getSubjectCode(),
                offering.getSubject().getSubjectName(),
                offering.getSemester().getSemesterId(),
                offering.getSemester().getName(),
                academicGroupService.toResponse(offering.getAcademicGroup()),
                offering.getSchedules().stream()
                        .sorted(Comparator
                                .comparing(Schedule::getDayOfWeek)
                                .thenComparing(Schedule::getStartTime)
                                .thenComparing(Schedule::getEndTime)
                                .thenComparing(Schedule::getScheduleId))
                        .map(this::toScheduleResponse)
                        .toList(),
                enrolled
        );
    }

    private ScheduleResponse toScheduleResponse(Schedule schedule) {
        CourseOffering offering = schedule.getCourseOffering();
        Room room = schedule.getRoom();
        return new ScheduleResponse(
                schedule.getScheduleId(),
                offering.getOfferingId(),
                offering.getSemester().getSemesterId(),
                offering.getSemester().getName(),
                academicGroupService.toResponse(offering.getAcademicGroup()),
                offering.getSubject().getSubjectId(),
                offering.getSubject().getSubjectCode(),
                offering.getSubject().getSubjectName(),
                room.getRoomId(),
                room.getRoomNumber(),
                room.getRoomName(),
                room.getFloor().getBuilding().getBuildingId(),
                room.getFloor().getBuilding().getBuildingName(),
                room.getFloor().getFloorId(),
                room.getFloor().getFloorNumber(),
                schedule.getDayOfWeek(),
                DayOfWeek.of(schedule.getDayOfWeek()).name(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                offering.getType()
        );
    }
}
