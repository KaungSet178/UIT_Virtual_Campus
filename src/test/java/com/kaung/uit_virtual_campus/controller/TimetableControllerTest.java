package com.kaung.uit_virtual_campus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaung.uit_virtual_campus.dto.AcademicGroupResponse;
import com.kaung.uit_virtual_campus.dto.CourseOfferingRequest;
import com.kaung.uit_virtual_campus.dto.CourseOfferingResponse;
import com.kaung.uit_virtual_campus.dto.ScheduleRequest;
import com.kaung.uit_virtual_campus.dto.ScheduleResponse;
import com.kaung.uit_virtual_campus.dto.SemesterResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.service.AcademicGroupService;
import com.kaung.uit_virtual_campus.service.CourseOfferingService;
import com.kaung.uit_virtual_campus.service.ScheduleService;
import com.kaung.uit_virtual_campus.service.SemesterService;
import com.kaung.uit_virtual_campus.service.SubjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TimetableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SemesterService semesterService;

    @MockitoBean
    private AcademicGroupService academicGroupService;

    @MockitoBean
    private SubjectService subjectService;

    @MockitoBean
    private CourseOfferingService courseOfferingService;

    @MockitoBean
    private ScheduleService scheduleService;

    @Test
    @DisplayName("Unauthenticated timetable request returns 401")
    void unauthenticatedTimetableReturns401() throws Exception {
        mockMvc.perform(get("/api/timetable"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated personalized timetable request returns 401")
    void unauthenticatedPersonalTimetableReturns401() throws Exception {
        mockMvc.perform(get("/api/timetable/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can list semesters")
    void authenticatedUserCanListSemesters() throws Exception {
        when(semesterService.getAllSemesters()).thenReturn(List.of(new SemesterResponse(
                1L,
                "2026 Semester 1",
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 5, 29)
        )));

        mockMvc.perform(get("/api/semesters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].semesterId").value(1))
                .andExpect(jsonPath("$[0].name").value("2026 Semester 1"));
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can filter timetable by academic group")
    void authenticatedUserCanFilterTimetable() throws Exception {
        ScheduleResponse response = scheduleResponse();
        when(scheduleService.getTimetable(1L, 2L, 3L, 10L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/timetable")
                        .param("semesterId", "1")
                        .param("academicGroupId", "2")
                        .param("subjectId", "3")
                        .param("roomId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scheduleId").value(20))
                .andExpect(jsonPath("$[0].dayName").value("MONDAY"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Student can get personalized timetable")
    void studentCanGetPersonalizedTimetable() throws Exception {
        when(scheduleService.getCurrentStudentTimetable()).thenReturn(List.of(scheduleResponse()));

        mockMvc.perform(get("/api/timetable/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scheduleId").value(20))
                .andExpect(jsonPath("$[0].academicGroup.groupId").value(2));
    }

    @Test
    @WithMockUser(roles = "CLUB_LEADER")
    @DisplayName("Club leader can get personalized timetable when profile exists")
    void clubLeaderCanGetPersonalizedTimetable() throws Exception {
        when(scheduleService.getCurrentStudentTimetable()).thenReturn(List.of(scheduleResponse()));

        mockMvc.perform(get("/api/timetable/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].scheduleId").value(20));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin-only user cannot get personalized timetable")
    void adminOnlyCannotGetPersonalizedTimetable() throws Exception {
        mockMvc.perform(get("/api/timetable/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("Non-admin user cannot create course offering")
    void nonAdminCannotCreateCourseOffering() throws Exception {
        CourseOfferingRequest request = new CourseOfferingRequest(1L, 2L, 3L);

        mockMvc.perform(post("/api/course-offerings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can create course offering")
    void adminCanCreateCourseOffering() throws Exception {
        CourseOfferingRequest request = new CourseOfferingRequest(1L, 2L, 3L);
        when(courseOfferingService.createCourseOffering(any(CourseOfferingRequest.class))).thenReturn(
                new CourseOfferingResponse(
                        4L,
                        1L,
                        "2026 Semester 1",
                        academicGroupResponse(),
                        3L,
                        "CS101",
                        "Programming Fundamentals"
                )
        );

        mockMvc.perform(post("/api/course-offerings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.offeringId").value(4));
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can list academic groups")
    void authenticatedUserCanListAcademicGroups() throws Exception {
        when(academicGroupService.getAcademicGroups((short) 3, AcademicGroupType.MAJOR, "CS"))
                .thenReturn(List.of(academicGroupResponse()));

        mockMvc.perform(get("/api/academic-groups")
                        .param("year", "3")
                        .param("groupType", "MAJOR")
                        .param("department", "CS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value(2))
                .andExpect(jsonPath("$[0].groupType").value("MAJOR"))
                .andExpect(jsonPath("$[0].major").value("SE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin create schedule validates request body")
    void adminCreateScheduleValidatesRequest() throws Exception {
        ScheduleRequest request = new ScheduleRequest(4L, 10L, (short) 8, LocalTime.of(9, 0), LocalTime.of(10, 0));

        mockMvc.perform(post("/api/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.dayOfWeek").value("must be less than or equal to 7"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can delete schedule")
    void adminCanDeleteSchedule() throws Exception {
        mockMvc.perform(delete("/api/schedules/20"))
                .andExpect(status().isNoContent());
    }

    private ScheduleResponse scheduleResponse() {
        return new ScheduleResponse(
                20L,
                4L,
                1L,
                "2026 Semester 1",
                academicGroupResponse(),
                3L,
                "CS101",
                "Programming Fundamentals",
                10L,
                "111",
                "Room 111",
                1L,
                "Building 1",
                101L,
                1,
                (short) 1,
                "MONDAY",
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );
    }

    private AcademicGroupResponse academicGroupResponse() {
        return new AcademicGroupResponse(2L, (short) 3, AcademicGroupType.MAJOR, null, "CS", "SE");
    }
}
