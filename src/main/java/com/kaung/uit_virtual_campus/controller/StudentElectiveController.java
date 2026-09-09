package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.ElectiveCourseOfferingResponse;
import com.kaung.uit_virtual_campus.dto.ElectiveEnrollmentResponse;
import com.kaung.uit_virtual_campus.service.StudentElectiveService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/electives/me")
@PreAuthorize("hasRole('STUDENT')")
public class StudentElectiveController {

    private final StudentElectiveService studentElectiveService;

    public StudentElectiveController(StudentElectiveService studentElectiveService) {
        this.studentElectiveService = studentElectiveService;
    }

    @GetMapping("/available")
    public List<ElectiveCourseOfferingResponse> listAvailableElectives() {
        return studentElectiveService.getAvailableElectives();
    }

    @GetMapping
    public List<ElectiveEnrollmentResponse> listMyElectives() {
        return studentElectiveService.getCurrentEnrollments();
    }

    @PostMapping("/{courseOfferingId}")
    public ResponseEntity<ElectiveEnrollmentResponse> enroll(@PathVariable Long courseOfferingId) {
        ElectiveEnrollmentResponse response = studentElectiveService.enroll(courseOfferingId);
        return ResponseEntity
                .created(URI.create("/api/electives/me/" + response.courseOffering().courseOfferingId()))
                .body(response);
    }

    @DeleteMapping("/{courseOfferingId}")
    public ResponseEntity<Void> removeEnrollment(@PathVariable Long courseOfferingId) {
        studentElectiveService.removeEnrollment(courseOfferingId);
        return ResponseEntity.noContent().build();
    }
}
