package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.SubjectResponse;
import com.kaung.uit_virtual_campus.entity.Subject;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findAllByOrderBySubjectCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public SubjectResponse getSubjectById(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + subjectId));
        return toResponse(subject);
    }

    private SubjectResponse toResponse(Subject subject) {
        return new SubjectResponse(
                subject.getSubjectId(),
                subject.getSubjectCode(),
                subject.getSubjectName(),
                subject.getDescription()
        );
    }
}
