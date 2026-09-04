package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.dto.CurrentUserResponse;
import com.kaung.uit_virtual_campus.dto.StaffProfileResponse;
import com.kaung.uit_virtual_campus.dto.StudentProfileResponse;
import com.kaung.uit_virtual_campus.dto.StudentOnboardingRequest;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.service.CurrentUserProfileService;
import com.kaung.uit_virtual_campus.service.StudentOnboardingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserProfileService currentUserProfileService;

    @MockitoBean
    private StudentOnboardingService studentOnboardingService;

    @Test
    @DisplayName("GET /api/me requires authentication")
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/me"));
    }

    @Test
    @DisplayName("POST /api/me/student-profile requires authentication")
    void onboardingRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/me/student-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Student User",
                                  "studentNumber": "UIT-001",
                                  "academicGroupId": 1
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.path").value("/api/me/student-profile"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/me returns identity roles and student profile")
    void meReturnsCurrentUserProfile() throws Exception {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(new CurrentUserResponse(
                userId,
                "student@uit.example",
                "Student User",
                "https://example.test/profile.png",
                "+95 9 123 456 789",
                List.of("CLUB_LEADER", "STUDENT"),
                new StudentProfileResponse(
                        "UIT-001",
                        "CS",
                        "Software Engineering",
                        (short) 3,
                        new AcademicGroupResponse(5L, (short) 3, AcademicGroupType.MAJOR, null, "CS", "SE")
                ),
                null
        ));

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("student@uit.example"))
                .andExpect(jsonPath("$.fullName").value("Student User"))
                .andExpect(jsonPath("$.profileImage").value("https://example.test/profile.png"))
                .andExpect(jsonPath("$.phone").value("+95 9 123 456 789"))
                .andExpect(jsonPath("$.roles[0]").value("CLUB_LEADER"))
                .andExpect(jsonPath("$.roles[1]").value("STUDENT"))
                .andExpect(jsonPath("$.studentProfile.studentNumber").value("UIT-001"))
                .andExpect(jsonPath("$.studentProfile.department").value("CS"))
                .andExpect(jsonPath("$.studentProfile.programme").value("Software Engineering"))
                .andExpect(jsonPath("$.studentProfile.year").value(3))
                .andExpect(jsonPath("$.studentProfile.academicGroup.groupId").value(5))
                .andExpect(jsonPath("$.studentProfile.academicGroup.groupType").value("MAJOR"))
                .andExpect(jsonPath("$.studentProfile.academicGroup.department").value("CS"))
                .andExpect(jsonPath("$.studentProfile.academicGroup.major").value("SE"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/me/student-profile completes student onboarding")
    void onboardingCompletesStudentProfile() throws Exception {
        UUID userId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        when(studentOnboardingService.completeStudentOnboarding(any(StudentOnboardingRequest.class)))
                .thenReturn(new CurrentUserResponse(
                        userId,
                        "new.student@uit.example",
                "New Student",
                null,
                null,
                List.of("STUDENT"),
                new StudentProfileResponse(
                        "UIT-007",
                                null,
                                null,
                        (short) 1,
                        new AcademicGroupResponse(1L, (short) 1, AcademicGroupType.SECTION, "A", null, null)
                ),
                null
        ));

        mockMvc.perform(post("/api/me/student-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "New Student",
                                  "studentNumber": "UIT-007",
                                  "academicGroupId": 1,
                                  "role": "ADMIN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("STUDENT"))
                .andExpect(jsonPath("$.studentProfile.studentNumber").value("UIT-007"))
                .andExpect(jsonPath("$.studentProfile.academicGroup.groupType").value("SECTION"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/me returns section academic group")
    void meReturnsSectionAcademicGroup() throws Exception {
        UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(new CurrentUserResponse(
                userId,
                "student@uit.example",
                "Student User",
                null,
                null,
                List.of("STUDENT"),
                new StudentProfileResponse(
                        "UIT-002",
                        null,
                        null,
                        (short) 2,
                        new AcademicGroupResponse(3L, (short) 2, AcademicGroupType.SECTION, "A", null, null)
                ),
                null
        ));

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentProfile.academicGroup.groupId").value(3))
                .andExpect(jsonPath("$.studentProfile.academicGroup.groupType").value("SECTION"))
                .andExpect(jsonPath("$.studentProfile.academicGroup.sectionName").value("A"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/me returns null academic group when unassigned")
    void meReturnsNullAcademicGroup() throws Exception {
        UUID userId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(new CurrentUserResponse(
                userId,
                "student@uit.example",
                "Student User",
                null,
                null,
                List.of("STUDENT"),
                new StudentProfileResponse("UIT-003", "CS", "Software Engineering", (short) 3, null),
                null
        ));

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentProfile.academicGroup").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/me returns staff profile for admin users")
    void meReturnsStaffProfile() throws Exception {
        UUID userId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(new CurrentUserResponse(
                userId,
                "admin@uit.example",
                "Admin User",
                null,
                null,
                List.of("ADMIN"),
                null,
                new StaffProfileResponse("UIT-STF-001", "Student Affairs", "Administrator")
        ));

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.studentProfile").doesNotExist())
                .andExpect(jsonPath("$.staffProfile.employeeNumber").value("UIT-STF-001"))
                .andExpect(jsonPath("$.staffProfile.department").value("Student Affairs"))
                .andExpect(jsonPath("$.staffProfile.position").value("Administrator"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/me allows admin users without configured staff profile")
    void meAllowsMissingStaffProfile() throws Exception {
        UUID userId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(currentUserProfileService.getCurrentUserProfile()).thenReturn(new CurrentUserResponse(
                userId,
                "admin@uit.example",
                "Admin User",
                null,
                null,
                List.of("ADMIN"),
                null,
                null
        ));

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.staffProfile").doesNotExist());
    }
}
