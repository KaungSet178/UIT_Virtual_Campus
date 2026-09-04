package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.CourseOfferingRequest;
import com.kaung.uit_virtual_campus.dto.CourseOfferingResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.Subject;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.AcademicGroupRepository;
import com.kaung.uit_virtual_campus.repository.CourseOfferingRepository;
import com.kaung.uit_virtual_campus.repository.SemesterRepository;
import com.kaung.uit_virtual_campus.repository.SubjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CourseOfferingService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final SemesterRepository semesterRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final SubjectRepository subjectRepository;
    private final AcademicGroupService academicGroupService;

    public CourseOfferingService(
            CourseOfferingRepository courseOfferingRepository,
            SemesterRepository semesterRepository,
            AcademicGroupRepository academicGroupRepository,
            SubjectRepository subjectRepository,
            AcademicGroupService academicGroupService
    ) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.semesterRepository = semesterRepository;
        this.academicGroupRepository = academicGroupRepository;
        this.subjectRepository = subjectRepository;
        this.academicGroupService = academicGroupService;
    }

    public List<CourseOfferingResponse> getCourseOfferings(Long semesterId, Long academicGroupId, Long subjectId) {
        return courseOfferingRepository.findAllWithFilters(semesterId, academicGroupId, subjectId).stream()
                .map(this::toResponse)
                .toList();
    }

    public CourseOfferingResponse getCourseOfferingById(Long offeringId) {
        CourseOffering courseOffering = courseOfferingRepository.findByIdWithDetails(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found with id: " + offeringId));
        return toResponse(courseOffering);
    }

    @Transactional
    public CourseOfferingResponse createCourseOffering(CourseOfferingRequest request) {
        Semester semester = semesterRepository.findById(request.semesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Semester not found with id: " + request.semesterId()));
        AcademicGroup academicGroup = academicGroupRepository.findById(request.academicGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Academic group not found with id: " + request.academicGroupId()));
        Subject subject = subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found with id: " + request.subjectId()));

        if (courseOfferingRepository.existsBySemesterSemesterIdAndAcademicGroupGroupIdAndSubjectSubjectId(
                request.semesterId(), request.academicGroupId(), request.subjectId())) {
            throw new ConflictException("Course offering already exists for the semester, academic group, and subject.");
        }

        try {
            CourseOffering saved = courseOfferingRepository.save(new CourseOffering(semester, academicGroup, subject));
            return getCourseOfferingById(saved.getOfferingId());
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Course offering already exists for the semester, academic group, and subject.");
        }
    }

    @Transactional
    public void deleteCourseOffering(Long offeringId) {
        if (!courseOfferingRepository.existsById(offeringId)) {
            throw new ResourceNotFoundException("Course offering not found with id: " + offeringId);
        }
        courseOfferingRepository.deleteById(offeringId);
    }

    private CourseOfferingResponse toResponse(CourseOffering courseOffering) {
        return new CourseOfferingResponse(
                courseOffering.getOfferingId(),
                courseOffering.getSemester().getSemesterId(),
                courseOffering.getSemester().getName(),
                academicGroupService.toResponse(courseOffering.getAcademicGroup()),
                courseOffering.getSubject().getSubjectId(),
                courseOffering.getSubject().getSubjectCode(),
                courseOffering.getSubject().getSubjectName()
        );
    }
}
