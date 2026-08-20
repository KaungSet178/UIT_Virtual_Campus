package com.kaung.uit_virtual_campus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kaung.uit_virtual_campus.dto.ClubCreateRequest;
import com.kaung.uit_virtual_campus.dto.ClubGalleryCreateRequest;
import com.kaung.uit_virtual_campus.dto.ClubGalleryResponse;
import com.kaung.uit_virtual_campus.dto.ClubMemberResponse;
import com.kaung.uit_virtual_campus.dto.ClubResponse;
import com.kaung.uit_virtual_campus.dto.ClubUpdateRequest;
import com.kaung.uit_virtual_campus.dto.MyClubResponse;
import com.kaung.uit_virtual_campus.entity.ClubMembershipStatus;
import com.kaung.uit_virtual_campus.service.ClubGalleryService;
import com.kaung.uit_virtual_campus.service.ClubMembershipService;
import com.kaung.uit_virtual_campus.service.ClubService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClubService clubService;

    @MockitoBean
    private ClubMembershipService clubMembershipService;

    @MockitoBean
    private ClubGalleryService clubGalleryService;

    @Test
    @DisplayName("Unauthenticated club list returns 401")
    void unauthenticatedClubListReturns401() throws Exception {
        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated user can list clubs")
    void authenticatedCanListClubs() throws Exception {
        when(clubService.getAllClubs()).thenReturn(List.of(clubResponse()));

        mockMvc.perform(get("/api/clubs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clubId").value(10))
                .andExpect(jsonPath("$[0].clubName").value("Programming Club"));
    }

    @Test
    @WithMockUser
    @DisplayName("Public member list exposes minimal member information")
    void publicMemberListIsMinimal() throws Exception {
        when(clubMembershipService.getActiveMembers(10L)).thenReturn(List.of(new ClubMemberResponse(
                100L,
                "Member",
                OffsetDateTime.parse("2026-08-20T00:00:00Z")
        )));

        mockMvc.perform(get("/api/clubs/10/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Member"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].studentNumber").doesNotExist())
                .andExpect(jsonPath("$[0].department").doesNotExist());
    }

    @Test
    @WithMockUser
    @DisplayName("Authenticated student can join club")
    void authenticatedCanJoinClub() throws Exception {
        when(clubMembershipService.joinClub(10L)).thenReturn(new MyClubResponse(
                10L,
                "Programming Club",
                null,
                false,
                ClubMembershipStatus.ACTIVE,
                null
        ));

        mockMvc.perform(post("/api/clubs/10/join"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membershipStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    @DisplayName("Leader or admin path can update club profile")
    void updateClubProfile() throws Exception {
        ClubUpdateRequest request = new ClubUpdateRequest("Programming Club", "Updated", null, null, null);
        when(clubService.updateClub(any(Long.class), any(ClubUpdateRequest.class))).thenReturn(clubResponse());

        mockMvc.perform(put("/api/clubs/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value("Programming Club"));
    }

    @Test
    @WithMockUser
    @DisplayName("Leader or admin path can add gallery image URL")
    void addGalleryImageUrl() throws Exception {
        ClubGalleryCreateRequest request = new ClubGalleryCreateRequest("https://example.com/image.jpg", "Caption");
        when(clubGalleryService.addGalleryItem(any(Long.class), any(ClubGalleryCreateRequest.class))).thenReturn(
                new ClubGalleryResponse(50L, 10L, "https://example.com/image.jpg", "Caption", null)
        );

        mockMvc.perform(post("/api/clubs/10/gallery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.galleryId").value(50));
    }

    @Test
    @WithMockUser
    @DisplayName("Gallery image URL must be a valid HTTP URL")
    void galleryImageUrlMustBeValidHttpUrl() throws Exception {
        ClubGalleryCreateRequest request = new ClubGalleryCreateRequest("not-a-url", "Caption");

        mockMvc.perform(post("/api/clubs/10/gallery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(clubGalleryService);
    }

    @Test
    @WithMockUser
    @DisplayName("Non-admin cannot create club through admin endpoint")
    void nonAdminCannotCreateClub() throws Exception {
        ClubCreateRequest request = new ClubCreateRequest(UUID.randomUUID(), "Programming Club", "Desc", null, null, null);

        mockMvc.perform(post("/api/admin/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can create club")
    void adminCanCreateClub() throws Exception {
        ClubCreateRequest request = new ClubCreateRequest(UUID.randomUUID(), "Programming Club", "Desc", null, null, null);
        when(clubService.createClub(any(ClubCreateRequest.class))).thenReturn(clubResponse());

        mockMvc.perform(post("/api/admin/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clubId").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Admin can delete club")
    void adminCanDeleteClub() throws Exception {
        mockMvc.perform(delete("/api/admin/clubs/10"))
                .andExpect(status().isNoContent());
    }

    private ClubResponse clubResponse() {
        return new ClubResponse(
                10L,
                "Programming Club",
                "Desc",
                null,
                null,
                null,
                UUID.randomUUID(),
                "Leader",
                2L,
                null,
                null
        );
    }
}
