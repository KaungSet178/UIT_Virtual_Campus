package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.CourseOfferingRequest;
import com.kaung.uit_virtual_campus.dto.CourseOfferingResponse;
import com.kaung.uit_virtual_campus.entity.CourseOfferingType;
import com.kaung.uit_virtual_campus.service.CourseOfferingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/course-offerings")
public class CourseOfferingController {

    private final CourseOfferingService courseOfferingService;

    public CourseOfferingController(CourseOfferingService courseOfferingService) {
        this.courseOfferingService = courseOfferingService;
    }

    @GetMapping
    public List<CourseOfferingResponse> listCourseOfferings(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long academicGroupId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) CourseOfferingType type
    ) {
        return courseOfferingService.getCourseOfferings(semesterId, academicGroupId, subjectId, type);
    }

    @PutMapping("/{offeringId}")
    @PreAuthorize("hasRole('ADMIN')")
    public CourseOfferingResponse updateCourseOffering(
            @PathVariable Long offeringId,
            @Valid @RequestBody CourseOfferingRequest request
    ) {
        return courseOfferingService.updateCourseOffering(offeringId, request);
    }

    @GetMapping("/{offeringId}")
    public CourseOfferingResponse getCourseOffering(@PathVariable Long offeringId) {
        return courseOfferingService.getCourseOfferingById(offeringId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseOfferingResponse> createCourseOffering(
            @Valid @RequestBody CourseOfferingRequest request
    ) {
        CourseOfferingResponse response = courseOfferingService.createCourseOffering(request);
        return ResponseEntity
                .created(URI.create("/api/course-offerings/" + response.offeringId()))
                .body(response);
    }

    @DeleteMapping("/{offeringId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourseOffering(@PathVariable Long offeringId) {
        courseOfferingService.deleteCourseOffering(offeringId);
        return ResponseEntity.noContent().build();
    }
}
