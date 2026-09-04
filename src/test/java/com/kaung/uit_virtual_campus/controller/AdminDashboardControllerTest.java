package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.AdminDashboardSummaryResponse;
import com.kaung.uit_virtual_campus.service.AdminDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("GET /api/admin/dashboard requires authentication")
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/admin/dashboard"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("GET /api/admin/dashboard rejects students")
    void dashboardRejectsStudents() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/admin/dashboard"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /api/admin/dashboard returns summary counts")
    void dashboardReturnsSummaryCounts() throws Exception {
        when(adminDashboardService.getSummary()).thenReturn(new AdminDashboardSummaryResponse(
                2L,
                1L,
                144L,
                3L,
                4L,
                5L,
                112L
        ));

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.totalStudents").value(1))
                .andExpect(jsonPath("$.totalRooms").value(144))
                .andExpect(jsonPath("$.pendingReservations").value(3))
                .andExpect(jsonPath("$.totalClubs").value(4))
                .andExpect(jsonPath("$.upcomingEvents").value(5))
                .andExpect(jsonPath("$.totalSchedules").value(112));
    }
}
