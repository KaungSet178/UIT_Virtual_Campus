package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.service.AcademicGroupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/academic-groups")
public class AcademicGroupController {

    private final AcademicGroupService academicGroupService;

    public AcademicGroupController(AcademicGroupService academicGroupService) {
        this.academicGroupService = academicGroupService;
    }

    @GetMapping
    public List<AcademicGroupResponse> listAcademicGroups(
            @RequestParam(required = false) Short year,
            @RequestParam(required = false) AcademicGroupType groupType,
            @RequestParam(required = false) String department
    ) {
        return academicGroupService.getAcademicGroups(year, groupType, department);
    }

    @GetMapping("/{groupId}")
    public AcademicGroupResponse getAcademicGroup(@PathVariable Long groupId) {
        return academicGroupService.getAcademicGroupById(groupId);
    }
}
