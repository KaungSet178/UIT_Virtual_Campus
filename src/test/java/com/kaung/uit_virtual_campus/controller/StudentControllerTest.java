package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.dto.StudentListResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.service.StudentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    @DisplayName("GET /api/students requires authentication")
    void studentListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/students"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/students rejects students")
    void studentListRejectsStudents() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/students"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/students allows admins")
    void studentListAllowsAdmins() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(studentService.listStudents(null, null, null, null)).thenReturn(List.of(
                new StudentListResponse(
                        userId,
                        "ksthar@uit.edu.mm",
                        "Kaung Set Thar",
                        null,
                        null,
                        "UIT-TEST-001",
                        (short) 1,
                        null,
                        null,
                        new AcademicGroupResponse(1L, (short) 1, AcademicGroupType.SECTION, "A", null, null)
                )
        ));

        mockMvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].email").value("ksthar@uit.edu.mm"))
                .andExpect(jsonPath("$[0].fullName").value("Kaung Set Thar"))
                .andExpect(jsonPath("$[0].studentNumber").value("UIT-TEST-001"))
                .andExpect(jsonPath("$[0].year").value(1))
                .andExpect(jsonPath("$[0].academicGroup.groupId").value(1))
                .andExpect(jsonPath("$[0].academicGroup.groupType").value("SECTION"))
                .andExpect(jsonPath("$[0].academicGroup.sectionName").value("A"));
    }

    @Test
    @WithMockUser(roles = "CLUB_LEADER")
    @DisplayName("GET /api/students allows club leaders")
    void studentListAllowsClubLeaders() throws Exception {
        when(studentService.listStudents("kaung", (short) 1, 1L, "CS")).thenReturn(List.of());

        mockMvc.perform(get("/api/students")
                        .param("query", "kaung")
                        .param("year", "1")
                        .param("academicGroupId", "1")
                        .param("department", "CS"))
                .andExpect(status().isOk());
    }
}
