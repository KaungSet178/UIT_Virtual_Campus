package com.kaung.uit_virtual_campus.dto;

import java.time.LocalDateTime;

public record ElectiveEnrollmentResponse(
        Long enrollmentId,
        LocalDateTime createdAt,
        ElectiveCourseOfferingResponse courseOffering
) {
}
