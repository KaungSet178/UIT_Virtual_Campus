package com.kaung.uit_virtual_campus.dto;

import jakarta.validation.constraints.NotNull;

public record CourseOfferingRequest(
        @NotNull Long semesterId,
        @NotNull Long academicGroupId,
        @NotNull Long subjectId
) {
}
