package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.dto.StudentListResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentService {

    private final UserRepository userRepository;

    public StudentService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<StudentListResponse> listStudents(
            String query,
            Short year,
            Long academicGroupId,
            String department
    ) {
        return userRepository.findStudentsForList(
                        normalize(query),
                        year,
                        academicGroupId,
                        normalize(department)
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private StudentListResponse toResponse(User user) {
        StudentProfile studentProfile = user.getStudentProfile();
        AcademicGroup academicGroup = studentProfile == null ? null : studentProfile.getAcademicGroup();
        return new StudentListResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getProfileImage(),
                user.getPhone(),
                studentProfile == null ? null : studentProfile.getStudentNumber(),
                studentProfile == null ? null : studentProfile.getYear(),
                studentProfile == null ? null : studentProfile.getDepartment(),
                studentProfile == null ? null : studentProfile.getProgramme(),
                academicGroup == null ? null : new AcademicGroupResponse(
                        academicGroup.getGroupId(),
                        academicGroup.getYear(),
                        academicGroup.getGroupType(),
                        academicGroup.getSectionName(),
                        academicGroup.getDepartment(),
                        academicGroup.getMajor()
                )
        );
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
