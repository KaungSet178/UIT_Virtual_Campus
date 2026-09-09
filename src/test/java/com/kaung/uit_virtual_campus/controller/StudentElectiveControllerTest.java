package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.ElectiveCourseOfferingResponse;
import com.kaung.uit_virtual_campus.dto.ElectiveEnrollmentResponse;
import com.kaung.uit_virtual_campus.entity.CourseOfferingType;
import com.kaung.uit_virtual_campus.service.StudentElectiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentElectiveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentElectiveService studentElectiveService;

    @Test
    @DisplayName("Anonymous elective request returns 401")
    void anonymousElectiveRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/electives/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Anonymous signup elective availability request is allowed")
    void anonymousSignupAvailabilityRequestIsAllowed() throws Exception {
        when(studentElectiveService.getAvailableElectives(2L)).thenReturn(List.of(
                new ElectiveCourseOfferingResponse(
                        10L,
                        CourseOfferingType.ELECTIVE,
                        20L,
                        "CST-4105",
                        "Enterprise Applications Development using Java (Keystone Project)",
                        30L,
                        "2026 Semester 1",
                        null,
                        List.of(),
                        false
                )
        ));

        mockMvc.perform(get("/api/electives/available").param("academicGroupId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseOfferingId").value(10))
                .andExpect(jsonPath("$[0].enrolled").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin-only user cannot access student elective endpoints")
    void adminOnlyCannotAccessStudentElectives() throws Exception {
        mockMvc.perform(get("/api/electives/me"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Student can list available electives")
    void studentCanListAvailableElectives() throws Exception {
        when(studentElectiveService.getAvailableElectives()).thenReturn(List.of(
                new ElectiveCourseOfferingResponse(
                        10L,
                        CourseOfferingType.ELECTIVE,
                        20L,
                        "CST-4105",
                        "Enterprise Applications Development using Java (Keystone Project)",
                        30L,
                        "2026 Semester 1",
                        null,
                        List.of(),
                        true
                )
        ));

        mockMvc.perform(get("/api/electives/me/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseOfferingId").value(10))
                .andExpect(jsonPath("$[0].type").value("ELECTIVE"))
                .andExpect(jsonPath("$[0].subjectCode").value("CST-4105"))
                .andExpect(jsonPath("$[0].enrolled").value(true));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Student can enroll and remove own elective")
    void studentCanEnrollAndRemoveOwnElective() throws Exception {
        when(studentElectiveService.enroll(10L)).thenReturn(new ElectiveEnrollmentResponse(
                50L,
                null,
                new ElectiveCourseOfferingResponse(
                        10L,
                        CourseOfferingType.ELECTIVE,
                        20L,
                        "CST-4408",
                        "Foundations of Cybersecurity",
                        30L,
                        "2026 Semester 1",
                        null,
                        List.of(),
                        true
                )
        ));

        mockMvc.perform(post("/api/electives/me/10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").value(50))
                .andExpect(jsonPath("$.courseOffering.subjectCode").value("CST-4408"));

        mockMvc.perform(delete("/api/electives/me/10"))
                .andExpect(status().isNoContent());
    }
}
