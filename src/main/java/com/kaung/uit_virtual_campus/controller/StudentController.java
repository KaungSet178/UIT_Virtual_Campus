package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.StudentListResponse;
import com.kaung.uit_virtual_campus.service.StudentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
@PreAuthorize("hasAnyRole('ADMIN', 'CLUB_LEADER')")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public List<StudentListResponse> listStudents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Short year,
            @RequestParam(required = false) Long academicGroupId,
            @RequestParam(required = false) String department
    ) {
        return studentService.listStudents(query, year, academicGroupId, department);
    }
}
