package com.kaung.uit_virtual_campus.dto;

import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;

public record StudentProfileResponse(
        String studentNumber,
        String department,
        String programme,
        Short year,
        AcademicGroupResponse academicGroup
) {
}
