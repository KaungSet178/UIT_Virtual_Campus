package com.kaung.uit_virtual_campus.dto;

import com.kaung.uit_virtual_campus.entity.CourseOfferingType;

import java.util.List;

public record ElectiveCourseOfferingResponse(
        Long courseOfferingId,
        CourseOfferingType type,
        Long subjectId,
        String subjectCode,
        String subjectName,
        Long semesterId,
        String semesterName,
        AcademicGroupResponse academicGroup,
        List<ScheduleResponse> schedules,
        boolean enrolled
) {
}
