package com.kaung.uit_virtual_campus.dto;

import com.kaung.uit_virtual_campus.entity.AcademicGroupType;

public record AcademicGroupResponse(
        Long groupId,
        Short year,
        AcademicGroupType groupType,
        String sectionName,
        String department,
        String major
) {
}
