package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TestAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @Test
    void publicEndpointIsAccessible() throws Exception {
        mockMvc.perform(get("/api/test/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("UIT Virtual Campus backend is reachable"));
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/test/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void currentUserEndpointReturnsSafeUserData() throws Exception {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(currentUserService.getCurrentUser()).thenReturn(new User(
                userId,
                "student@uit.example",
                "Student User",
                "https://example.test/profile.png",
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(new Role((short) 1, "STUDENT"))
        ));

        mockMvc.perform(get("/api/test/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("student@uit.example"))
                .andExpect(jsonPath("$.fullName").value("Student User"))
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"))
                .andExpect(jsonPath("$.profileImage").doesNotExist());
    }
}
