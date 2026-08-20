package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.SubjectResponse;
import com.kaung.uit_virtual_campus.service.SubjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public List<SubjectResponse> listSubjects() {
        return subjectService.getAllSubjects();
    }

    @GetMapping("/{subjectId}")
    public SubjectResponse getSubject(@PathVariable Long subjectId) {
        return subjectService.getSubjectById(subjectId);
    }
}
