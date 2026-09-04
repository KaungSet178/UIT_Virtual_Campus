package com.kaung.uit_virtual_campus.dto;

public record CourseOfferingResponse(
        Long offeringId,
        Long semesterId,
        String semesterName,
        AcademicGroupResponse academicGroup,
        Long subjectId,
        String subjectCode,
        String subjectName
) {
}
