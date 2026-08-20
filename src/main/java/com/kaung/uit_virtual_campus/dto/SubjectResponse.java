package com.kaung.uit_virtual_campus.dto;

public record SubjectResponse(
        Long subjectId,
        String subjectCode,
        String subjectName,
        String description
) {
}
