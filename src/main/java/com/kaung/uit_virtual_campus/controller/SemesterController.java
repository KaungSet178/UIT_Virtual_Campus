package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.SemesterResponse;
import com.kaung.uit_virtual_campus.service.SemesterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/semesters")
public class SemesterController {

    private final SemesterService semesterService;

    public SemesterController(SemesterService semesterService) {
        this.semesterService = semesterService;
    }

    @GetMapping
    public List<SemesterResponse> listSemesters() {
        return semesterService.getAllSemesters();
    }

    @GetMapping("/{semesterId}")
    public SemesterResponse getSemester(@PathVariable Long semesterId) {
        return semesterService.getSemesterById(semesterId);
    }
}
