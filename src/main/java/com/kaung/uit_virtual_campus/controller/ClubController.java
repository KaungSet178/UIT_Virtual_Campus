package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.ClubGalleryCreateRequest;
import com.kaung.uit_virtual_campus.dto.ClubGalleryResponse;
import com.kaung.uit_virtual_campus.dto.ClubMemberManagementResponse;
import com.kaung.uit_virtual_campus.dto.ClubMemberResponse;
import com.kaung.uit_virtual_campus.dto.ClubResponse;
import com.kaung.uit_virtual_campus.dto.ClubUpdateRequest;
import com.kaung.uit_virtual_campus.dto.MyClubResponse;
import com.kaung.uit_virtual_campus.service.ClubGalleryService;
import com.kaung.uit_virtual_campus.service.ClubMembershipService;
import com.kaung.uit_virtual_campus.service.ClubService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/clubs")
public class ClubController {

    private final ClubService clubService;
    private final ClubMembershipService clubMembershipService;
    private final ClubGalleryService clubGalleryService;

    public ClubController(
            ClubService clubService,
            ClubMembershipService clubMembershipService,
            ClubGalleryService clubGalleryService
    ) {
        this.clubService = clubService;
        this.clubMembershipService = clubMembershipService;
        this.clubGalleryService = clubGalleryService;
    }

    @GetMapping
    public List<ClubResponse> listClubs() {
        return clubService.getAllClubs();
    }

    @GetMapping("/me")
    public List<MyClubResponse> listMyClubs() {
        return clubMembershipService.getMyClubs();
    }

    @GetMapping("/{clubId}")
    public ClubResponse getClub(@PathVariable Long clubId) {
        return clubService.getClubById(clubId);
    }

    @PutMapping("/{clubId}")
    public ClubResponse updateClub(
            @PathVariable Long clubId,
            @Valid @RequestBody ClubUpdateRequest request
    ) {
        return clubService.updateClub(clubId, request);
    }

    @GetMapping("/{clubId}/members")
    public List<ClubMemberResponse> listClubMembers(@PathVariable Long clubId) {
        return clubMembershipService.getActiveMembers(clubId);
    }

    @GetMapping("/{clubId}/members/manage")
    public List<ClubMemberManagementResponse> listClubMembersForManagement(@PathVariable Long clubId) {
        return clubMembershipService.getMembersForManagement(clubId);
    }

    @PostMapping("/{clubId}/join")
    public MyClubResponse joinClub(@PathVariable Long clubId) {
        return clubMembershipService.joinClub(clubId);
    }

    @PatchMapping("/{clubId}/leave")
    public MyClubResponse leaveClub(@PathVariable Long clubId) {
        return clubMembershipService.leaveClub(clubId);
    }

    @GetMapping("/{clubId}/gallery")
    public List<ClubGalleryResponse> listGallery(@PathVariable Long clubId) {
        return clubGalleryService.getGallery(clubId);
    }

    @PostMapping("/{clubId}/gallery")
    public ResponseEntity<ClubGalleryResponse> addGalleryItem(
            @PathVariable Long clubId,
            @Valid @RequestBody ClubGalleryCreateRequest request
    ) {
        ClubGalleryResponse response = clubGalleryService.addGalleryItem(clubId, request);
        return ResponseEntity
                .created(URI.create("/api/clubs/" + clubId + "/gallery/" + response.galleryId()))
                .body(response);
    }

    @DeleteMapping("/{clubId}/gallery/{galleryId}")
    public ResponseEntity<Void> deleteGalleryItem(
            @PathVariable Long clubId,
            @PathVariable Long galleryId
    ) {
        clubGalleryService.deleteGalleryItem(clubId, galleryId);
        return ResponseEntity.noContent().build();
    }
}
