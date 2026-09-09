package com.kaung.uit_virtual_campus.dto;

import com.kaung.uit_virtual_campus.entity.CourseOfferingType;

public record CourseOfferingResponse(
        Long offeringId,
        Long semesterId,
        String semesterName,
        AcademicGroupResponse academicGroup,
        Long subjectId,
        String subjectCode,
        String subjectName,
        CourseOfferingType type
) {
    public CourseOfferingResponse(
            Long offeringId,
            Long semesterId,
            String semesterName,
            AcademicGroupResponse academicGroup,
            Long subjectId,
            String subjectCode,
            String subjectName
    ) {
        this(
                offeringId,
                semesterId,
                semesterName,
                academicGroup,
                subjectId,
                subjectCode,
                subjectName,
                CourseOfferingType.MANDATORY
        );
    }
}
