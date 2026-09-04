package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.CurrentUserResponse;
import com.kaung.uit_virtual_campus.dto.StudentOnboardingRequest;
import com.kaung.uit_virtual_campus.service.CurrentUserProfileService;
import com.kaung.uit_virtual_campus.service.StudentOnboardingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUserProfileService currentUserProfileService;
    private final StudentOnboardingService studentOnboardingService;

    public MeController(
            CurrentUserProfileService currentUserProfileService,
            StudentOnboardingService studentOnboardingService
    ) {
        this.currentUserProfileService = currentUserProfileService;
        this.studentOnboardingService = studentOnboardingService;
    }

    @GetMapping
    public CurrentUserResponse currentUser() {
        return currentUserProfileService.getCurrentUserProfile();
    }

    @PostMapping("/student-profile")
    public CurrentUserResponse completeStudentOnboarding(@Valid @RequestBody StudentOnboardingRequest request) {
        return studentOnboardingService.completeStudentOnboarding(request);
    }
}
