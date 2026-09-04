package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.CourseOfferingRequest;
import com.kaung.uit_virtual_campus.dto.CourseOfferingResponse;
import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.Subject;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.AcademicGroupRepository;
import com.kaung.uit_virtual_campus.repository.CourseOfferingRepository;
import com.kaung.uit_virtual_campus.repository.SemesterRepository;
import com.kaung.uit_virtual_campus.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseOfferingServiceTest {

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private SemesterRepository semesterRepository;

    @Mock
    private AcademicGroupRepository academicGroupRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private AcademicGroupService academicGroupService;

    private CourseOfferingService courseOfferingService;
    private Semester semester;
    private AcademicGroup academicGroup;
    private AcademicGroupResponse academicGroupResponse;
    private Subject subject;
    private CourseOffering offering;

    @BeforeEach
    void setUp() {
        courseOfferingService = new CourseOfferingService(
                courseOfferingRepository,
                semesterRepository,
                academicGroupRepository,
                subjectRepository,
                academicGroupService
        );
        semester = new Semester(1L, "2026 Semester 1", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 5, 29));
        academicGroup = new AcademicGroup(2L, (short) 1, AcademicGroupType.SECTION, "A", null, null);
        academicGroupResponse = new AcademicGroupResponse(2L, (short) 1, AcademicGroupType.SECTION, "A", null, null);
        subject = new Subject(3L, "CS101", "Programming Fundamentals", "Intro");
        offering = new CourseOffering(4L, semester, academicGroup, subject);
    }

    @Test
    @DisplayName("Should list filtered course offerings")
    void listCourseOfferings() {
        when(courseOfferingRepository.findAllWithFilters(1L, 2L, 3L)).thenReturn(List.of(offering));
        when(academicGroupService.toResponse(academicGroup)).thenReturn(academicGroupResponse);

        List<CourseOfferingResponse> result = courseOfferingService.getCourseOfferings(1L, 2L, 3L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).offeringId()).isEqualTo(4L);
        assertThat(result.get(0).semesterName()).isEqualTo("2026 Semester 1");
        assertThat(result.get(0).academicGroup().sectionName()).isEqualTo("A");
        assertThat(result.get(0).subjectCode()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("Should create course offering when unique")
    void createCourseOffering() {
        CourseOfferingRequest request = new CourseOfferingRequest(1L, 2L, 3L);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(academicGroupRepository.findById(2L)).thenReturn(Optional.of(academicGroup));
        when(subjectRepository.findById(3L)).thenReturn(Optional.of(subject));
        when(courseOfferingRepository.existsBySemesterSemesterIdAndAcademicGroupGroupIdAndSubjectSubjectId(1L, 2L, 3L))
                .thenReturn(false);
        when(courseOfferingRepository.save(any(CourseOffering.class))).thenReturn(offering);
        when(courseOfferingRepository.findByIdWithDetails(4L)).thenReturn(Optional.of(offering));
        when(academicGroupService.toResponse(academicGroup)).thenReturn(academicGroupResponse);

        CourseOfferingResponse response = courseOfferingService.createCourseOffering(request);

        assertThat(response.offeringId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("Should reject duplicate course offering")
    void rejectDuplicateCourseOffering() {
        CourseOfferingRequest request = new CourseOfferingRequest(1L, 2L, 3L);
        when(semesterRepository.findById(1L)).thenReturn(Optional.of(semester));
        when(academicGroupRepository.findById(2L)).thenReturn(Optional.of(academicGroup));
        when(subjectRepository.findById(3L)).thenReturn(Optional.of(subject));
        when(courseOfferingRepository.existsBySemesterSemesterIdAndAcademicGroupGroupIdAndSubjectSubjectId(1L, 2L, 3L))
                .thenReturn(true);

        assertThatThrownBy(() -> courseOfferingService.createCourseOffering(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should reject missing semester")
    void rejectMissingSemester() {
        CourseOfferingRequest request = new CourseOfferingRequest(99L, 2L, 3L);
        when(semesterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseOfferingService.createCourseOffering(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Semester not found");
    }
}
