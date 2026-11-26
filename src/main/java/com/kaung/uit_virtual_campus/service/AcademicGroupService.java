package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.AcademicGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AcademicGroupService {

    private final AcademicGroupRepository academicGroupRepository;

    public AcademicGroupService(AcademicGroupRepository academicGroupRepository) {
        this.academicGroupRepository = academicGroupRepository;
    }

    public List<AcademicGroupResponse> getAcademicGroups(Short year, AcademicGroupType groupType, String department) {
        AcademicGroupType resolvedGroupType = groupType == null && year != null ? defaultGroupTypeForYear(year) : groupType;
        return academicGroupRepository.findAllWithFilters(year, resolvedGroupType, department).stream()
                .map(this::toResponse)
                .toList();
    }

    public AcademicGroupResponse getAcademicGroupById(Long groupId) {
        AcademicGroup academicGroup = academicGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic group not found with id: " + groupId));
        return toResponse(academicGroup);
    }

    public AcademicGroupResponse toResponse(AcademicGroup academicGroup) {
        return new AcademicGroupResponse(
                academicGroup.getGroupId(),
                academicGroup.getYear(),
                academicGroup.getGroupType(),
                academicGroup.getSectionName(),
                academicGroup.getDepartment(),
                academicGroup.getMajor()
        );
    }

    private AcademicGroupType defaultGroupTypeForYear(Short year) {
        if (year <= 2) {
            return AcademicGroupType.SECTION;
        }
        if (year == 3) {
            return AcademicGroupType.DEPARTMENT;
        }
        return AcademicGroupType.MAJOR;
    }
}
