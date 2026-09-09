package com.kaung.uit_virtual_campus.dto;

import com.kaung.uit_virtual_campus.entity.CourseOfferingType;
import jakarta.validation.constraints.NotNull;

public record CourseOfferingRequest(
        @NotNull Long semesterId,
        @NotNull Long academicGroupId,
        @NotNull Long subjectId,
        CourseOfferingType type
) {
    public CourseOfferingRequest(Long semesterId, Long academicGroupId, Long subjectId) {
        this(semesterId, academicGroupId, subjectId, null);
    }
}
