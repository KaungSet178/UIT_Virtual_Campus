package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.SemesterResponse;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.SemesterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SemesterService {

    private final SemesterRepository semesterRepository;

    public SemesterService(SemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    public List<SemesterResponse> getAllSemesters() {
        return semesterRepository.findAllByOrderByStartDateDescSemesterIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    public SemesterResponse getSemesterById(Long semesterId) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + semesterId));
        return toResponse(semester);
    }

    private SemesterResponse toResponse(Semester semester) {
        return new SemesterResponse(
                semester.getSemesterId(),
                semester.getName(),
                semester.getStartDate(),
                semester.getEndDate()
        );
    }
}
