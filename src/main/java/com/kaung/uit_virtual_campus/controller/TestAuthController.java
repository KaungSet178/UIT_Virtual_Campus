package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.CurrentUserResponse;
import com.kaung.uit_virtual_campus.dto.PublicTestResponse;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.service.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;

@RestController
@RequestMapping("/api/test")
public class TestAuthController {

    private final CurrentUserService currentUserService;

    public TestAuthController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/public")
    public PublicTestResponse publicEndpoint() {
        return new PublicTestResponse("UIT Virtual Campus backend is reachable");
    }

    /**
     * Temporary endpoint for initial Supabase JWT and application role verification.
     */
    @GetMapping("/me")
    public CurrentUserResponse currentUser() {
        User user = currentUserService.getCurrentUser();
        return new CurrentUserResponse(
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getRoles().stream()
                        .map(Role::getRoleName)
                        .sorted(Comparator.naturalOrder())
                        .toList()
        );
    }
}
