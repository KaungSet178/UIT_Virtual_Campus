package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.CurrentUserResponse;
import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.dto.StaffProfileResponse;
import com.kaung.uit_virtual_campus.dto.StudentProfileResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.StaffProfile;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.repository.StaffProfileRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;

@Service
@Transactional(readOnly = true)
public class CurrentUserProfileService {

    private final CurrentUserService currentUserService;
    private final StudentProfileRepository studentProfileRepository;
    private final StaffProfileRepository staffProfileRepository;

    public CurrentUserProfileService(
            CurrentUserService currentUserService,
            StudentProfileRepository studentProfileRepository,
            StaffProfileRepository staffProfileRepository
    ) {
        this.currentUserService = currentUserService;
        this.studentProfileRepository = studentProfileRepository;
        this.staffProfileRepository = staffProfileRepository;
    }

    public CurrentUserResponse getCurrentUserProfile() {
        User user = currentUserService.getCurrentUser();
        StudentProfileResponse studentProfile = studentProfileRepository.findWithUserByStudentId(user.getUserId())
                .map(this::toStudentProfileResponse)
                .orElse(null);
        StaffProfileResponse staffProfile = staffProfileRepository.findWithUserByStaffId(user.getUserId())
                .map(this::toStaffProfileResponse)
                .orElse(null);

        return new CurrentUserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getProfileImage(),
                user.getPhone(),
                user.getRoles().stream()
                        .map(Role::getRoleName)
                        .sorted(Comparator.naturalOrder())
                        .toList(),
                studentProfile,
                staffProfile
        );
    }

    private StudentProfileResponse toStudentProfileResponse(StudentProfile studentProfile) {
        AcademicGroup academicGroup = studentProfile.getAcademicGroup();
        return new StudentProfileResponse(
                studentProfile.getStudentNumber(),
                studentProfile.getDepartment(),
                studentProfile.getProgramme(),
                studentProfile.getYear(),
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

    private StaffProfileResponse toStaffProfileResponse(StaffProfile staffProfile) {
        return new StaffProfileResponse(
                staffProfile.getEmployeeNumber(),
                staffProfile.getDepartment(),
                staffProfile.getPosition()
        );
    }
}
