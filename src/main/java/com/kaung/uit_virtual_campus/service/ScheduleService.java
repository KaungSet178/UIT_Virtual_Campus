package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ScheduleRequest;
import com.kaung.uit_virtual_campus.dto.ScheduleResponse;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.CourseOfferingRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final RoomRepository roomRepository;
    private final TimetableValidationService validationService;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            CourseOfferingRepository courseOfferingRepository,
            RoomRepository roomRepository,
            TimetableValidationService validationService
    ) {
        this.scheduleRepository = scheduleRepository;
        this.courseOfferingRepository = courseOfferingRepository;
        this.roomRepository = roomRepository;
        this.validationService = validationService;
    }

    public List<ScheduleResponse> getTimetable(Long semesterId, Long sectionId, Long subjectId, Long roomId) {
        return scheduleRepository.findTimetable(semesterId, sectionId, subjectId, roomId).stream()
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
                offering.getSection().getSectionId(),
                offering.getSection().getSectionName(),
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
