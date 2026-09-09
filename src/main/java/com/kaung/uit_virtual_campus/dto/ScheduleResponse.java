package com.kaung.uit_virtual_campus.dto;

import com.kaung.uit_virtual_campus.entity.CourseOfferingType;

import java.time.LocalTime;

public record ScheduleResponse(
        Long scheduleId,
        Long offeringId,
        Long semesterId,
        String semesterName,
        AcademicGroupResponse academicGroup,
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
        LocalTime endTime,
        CourseOfferingType courseType
) {
    public ScheduleResponse(
            Long scheduleId,
            Long offeringId,
            Long semesterId,
            String semesterName,
            AcademicGroupResponse academicGroup,
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
        this(
                scheduleId,
                offeringId,
                semesterId,
                semesterName,
                academicGroup,
                subjectId,
                subjectCode,
                subjectName,
                roomId,
                roomNumber,
                roomName,
                buildingId,
                buildingName,
                floorId,
                floorNumber,
                dayOfWeek,
                dayName,
                startTime,
                endTime,
                CourseOfferingType.MANDATORY
        );
    }
}
