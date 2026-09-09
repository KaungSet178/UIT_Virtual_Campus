package com.kaung.uit_virtual_campus.dto;

import java.util.UUID;

public record StudentListResponse(
        UUID userId,
        String email,
        String fullName,
        String profileImage,
        String phone,
        String studentNumber,
        Short year,
        String department,
        String programme,
        AcademicGroupResponse academicGroup
) {
}
