package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class TimetableValidationService {

    private final ScheduleRepository scheduleRepository;

    public TimetableValidationService(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    public void validateScheduleTime(Short dayOfWeek, LocalTime startTime, LocalTime endTime) {
        if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek must be between 1 and 7 where 1=Monday and 7=Sunday.");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime.");
        }
    }

    public void validateNoRoomOverlap(Long roomId, Short dayOfWeek, LocalTime startTime,
                                      LocalTime endTime, Long semesterId, Long subjectId,
                                      Long academicGroupId, Long excludedScheduleId) {
        long overlaps = scheduleRepository.countDisallowedRoomOverlaps(
                roomId,
                dayOfWeek,
                startTime,
                endTime,
                semesterId,
                subjectId,
                academicGroupId,
                excludedScheduleId
        );
        if (overlaps > 0) {
            throw new ConflictException("Schedule overlaps an existing schedule for room id: " + roomId);
        }
    }
}
