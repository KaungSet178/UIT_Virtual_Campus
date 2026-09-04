package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ScheduleRequest;
import com.kaung.uit_virtual_campus.dto.ScheduleResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.CourseOfferingRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import com.kaung.uit_virtual_campus.repository.SemesterRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final RoomRepository roomRepository;
    private final TimetableValidationService validationService;
    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;
    private final SemesterRepository semesterRepository;
    private final Clock clock;
    private final AcademicGroupService academicGroupService;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            CourseOfferingRepository courseOfferingRepository,
            RoomRepository roomRepository,
            TimetableValidationService validationService,
            CurrentUserService currentUserService,
            StudentProfileRepository studentProfileRepository,
            SemesterRepository semesterRepository,
            Clock clock,
            AcademicGroupService academicGroupService
    ) {
        this.scheduleRepository = scheduleRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.roomRepository = roomRepository;
        this.validationService = validationService;
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
        this.semesterRepository = semesterRepository;
        this.clock = clock;
        this.academicGroupService = academicGroupService;
    }

    public List<ScheduleResponse> getTimetable(Long semesterId, Long academicGroupId, Long subjectId, Long roomId) {
        return scheduleRepository.findTimetable(semesterId, academicGroupId, subjectId, roomId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ScheduleResponse> getCurrentStudentTimetable() {
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
        if (academicGroup.getGroupType() == AcademicGroupType.MAJOR
                && studentProfile.getDepartment() != null
                && academicGroup.getDepartment() != null
                && !studentProfile.getDepartment().equalsIgnoreCase(academicGroup.getDepartment())) {
            throw new ConflictException("Student profile department does not match assigned academic group department.");
        }

        LocalDate today = LocalDate.now(clock);
        List<Semester> activeSemesters = semesterRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                        today,
                        today
                );
        if (activeSemesters.isEmpty()) {
            return List.of();
        }
        if (activeSemesters.size() > 1) {
            throw new ConflictException("Multiple active semesters are configured.");
        }

        Semester activeSemester = activeSemesters.get(0);
        return scheduleRepository.findPersonalTimetable(activeSemester.getSemesterId(), academicGroup.getGroupId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public ScheduleResponse getScheduleById(Long scheduleId) {
        Schedule schedule = scheduleRepository.findByIdWithDetails(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));
        return toResponse(schedule);
    }

    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest request) {
        CourseOffering courseOffering = courseOfferingRepository.findByIdWithDetails(request.offeringId())
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + request.offeringId()));
        Room room = roomRepository.findByIdForUpdate(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.roomId()));

        validationService.validateScheduleTime(request.dayOfWeek(), request.startTime(), request.endTime());
        validationService.validateNoRoomOverlap(
                request.roomId(),
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                courseOffering.getSemester().getSemesterId(),
                courseOffering.getSubject().getSubjectId(),
                courseOffering.getAcademicGroup().getGroupId(),
                null
        );

        Schedule saved = scheduleRepository.save(new Schedule(
                courseOffering,
                room,
                request.dayOfWeek(),
                request.startTime(),
                request.endTime()
        ));
        return getScheduleById(saved.getScheduleId());
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long scheduleId, ScheduleRequest request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found with id: " + scheduleId));
        CourseOffering courseOffering = courseOfferingRepository.findByIdWithDetails(request.offeringId())
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + request.offeringId()));
        Room room = roomRepository.findByIdForUpdate(request.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + request.roomId()));

        validationService.validateScheduleTime(request.dayOfWeek(), request.startTime(), request.endTime());
        validationService.validateNoRoomOverlap(
                request.roomId(),
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                courseOffering.getSemester().getSemesterId(),
                courseOffering.getSubject().getSubjectId(),
                courseOffering.getAcademicGroup().getGroupId(),
                scheduleId
        );

        schedule.update(courseOffering, room, request.dayOfWeek(), request.startTime(), request.endTime());
        return getScheduleById(schedule.getScheduleId());
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        if (!scheduleRepository.existsById(scheduleId)) {
            throw new ResourceNotFoundException("Schedule not found with id: " + scheduleId);
        }
        scheduleRepository.deleteById(scheduleId);
    }

    private ScheduleResponse toResponse(Schedule schedule) {
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
                dayName(schedule.getDayOfWeek()),
                schedule.getStartTime(),
                schedule.getEndTime()
        );
    }

    private String dayName(Short dayOfWeek) {
        return DayOfWeek.of(dayOfWeek).name();
    }
}
