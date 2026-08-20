package com.kaung.uit_virtual_campus.dto;

public record CourseOfferingResponse(
        Long offeringId,
        Long semesterId,
        String semesterName,
        Long sectionId,
        String sectionName,
        Long subjectId,
        String subjectCode,
        String subjectName
) {
}
