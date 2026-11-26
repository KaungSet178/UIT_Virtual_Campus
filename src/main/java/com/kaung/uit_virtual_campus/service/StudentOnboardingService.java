package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.CurrentUserResponse;
import com.kaung.uit_virtual_campus.dto.StudentOnboardingRequest;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.AcademicGroupRepository;
import com.kaung.uit_virtual_campus.repository.RoleRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class StudentOnboardingService {

    private final CurrentUserService currentUserService;
    private final CurrentUserProfileService currentUserProfileService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AcademicGroupRepository academicGroupRepository;
    private final StudentElectiveService studentElectiveService;

    public StudentOnboardingService(
            CurrentUserService currentUserService,
            CurrentUserProfileService currentUserProfileService,
            UserRepository userRepository,
            RoleRepository roleRepository,
            StudentProfileRepository studentProfileRepository,
            AcademicGroupRepository academicGroupRepository,
            StudentElectiveService studentElectiveService
    ) {
        this.currentUserService = currentUserService;
        this.currentUserProfileService = currentUserProfileService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.academicGroupRepository = academicGroupRepository;
        this.studentElectiveService = studentElectiveService;
    }

    @Transactional
    public CurrentUserResponse completeStudentOnboarding(StudentOnboardingRequest request) {
        UUID userId = currentUserService.getCurrentUserId();
        String fullName = request.fullName().trim();
        String studentNumber = request.studentNumber().trim();
        String phone = trimToNull(request.phone());

        User user = userRepository.findWithRolesByUserId(userId)
                .orElseGet(() -> new User(
                        userId,
                        currentUserService.getCurrentUserEmail()
                                .orElseThrow(() -> new IllegalArgumentException("Authenticated email is required.")),
                        fullName,
                        null,
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        Set.of()
                ));
        user.completeProfile(fullName, phone);
        user.addRole(studentRole());
        userRepository.save(user);

        if (studentProfileRepository.findWithUserByStudentId(userId).isPresent()) {
            return currentUserProfileService.getCurrentUserProfile();
        }

        studentProfileRepository.findByStudentNumber(studentNumber)
                .filter(existing -> !existing.getStudentId().equals(userId))
                .ifPresent(existing -> {
                    throw new ConflictException("Student number is already registered.");
                });

        AcademicGroup academicGroup = academicGroupRepository.findById(request.academicGroupId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Academic group not found with id: " + request.academicGroupId()
                ));
        StudentProfile studentProfile = studentProfileRepository.save(new StudentProfile(
                user,
                studentNumber,
                departmentFor(academicGroup),
                programmeFor(academicGroup),
                academicGroup.getYear(),
                academicGroup
        ));
        studentElectiveService.enrollForOnboarding(studentProfile, request.electiveCourseOfferingId());

        return currentUserProfileService.getCurrentUserProfile();
    }

    private Role studentRole() {
        return roleRepository.findByRoleNameIgnoreCase("STUDENT")
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: STUDENT"));
    }

    private String departmentFor(AcademicGroup academicGroup) {
        return academicGroup.getGroupType() == AcademicGroupType.SECTION ? null : academicGroup.getDepartment();
    }

    private String programmeFor(AcademicGroup academicGroup) {
        return academicGroup.getGroupType() == AcademicGroupType.MAJOR ? academicGroup.getMajor() : null;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
