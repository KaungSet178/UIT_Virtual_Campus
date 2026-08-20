package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.ClubCreateRequest;
import com.kaung.uit_virtual_campus.dto.ClubResponse;
import com.kaung.uit_virtual_campus.service.ClubService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/clubs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminClubController {

    private final ClubService clubService;

    public AdminClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @PostMapping
    public ResponseEntity<ClubResponse> createClub(@Valid @RequestBody ClubCreateRequest request) {
        ClubResponse response = clubService.createClub(request);
        return ResponseEntity
                .created(URI.create("/api/clubs/" + response.clubId()))
                .body(response);
    }

    @DeleteMapping("/{clubId}")
    public ResponseEntity<Void> deleteClub(@PathVariable Long clubId) {
        clubService.deleteClub(clubId);
        return ResponseEntity.noContent().build();
    }
}
