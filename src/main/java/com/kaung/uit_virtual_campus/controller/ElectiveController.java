package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.ElectiveCourseOfferingResponse;
import com.kaung.uit_virtual_campus.service.StudentElectiveService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/electives")
public class ElectiveController {

    private final StudentElectiveService studentElectiveService;

    public ElectiveController(StudentElectiveService studentElectiveService) {
        this.studentElectiveService = studentElectiveService;
    }

    @GetMapping("/available")
    public List<ElectiveCourseOfferingResponse> listAvailableElectives(
            @RequestParam Long academicGroupId
    ) {
        return studentElectiveService.getAvailableElectives(academicGroupId);
    }
}
