package com.kaung.uit_virtual_campus.dto;

import java.time.LocalTime;

public record ScheduleResponse(
        Long scheduleId,
        Long offeringId,
        Long semesterId,
        String semesterName,
        Long sectionId,
        String sectionName,
        Long subjectId,
        String subjectCode,
        String subjectName,
        Long roomId,
        String roomNumber,
        String roomName,
        Long buildingId,
        String buildingName,
        Long floorId,
        Integer floorNumber,
        Short dayOfWeek,
        String dayName,
        LocalTime startTime,
        LocalTime endTime
) {
}
