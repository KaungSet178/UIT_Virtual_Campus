package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ClubGalleryCreateRequest;
import com.kaung.uit_virtual_campus.dto.ClubGalleryResponse;
import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.ClubGallery;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.ClubGalleryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubGalleryServiceTest {

    @Mock
    private ClubService clubService;

    @Mock
    private ClubGalleryRepository clubGalleryRepository;

    @Mock
    private CurrentUserService currentUserService;

    private ClubGalleryService galleryService;
    private User leader;
    private User otherLeader;
    private User admin;
    private Club club;
    private Club otherClub;

    @BeforeEach
    void setUp() {
        ClubAuthorizationService auth = new ClubAuthorizationService();
        ClubMapper mapper = new ClubMapper();
        galleryService = new ClubGalleryService(
                clubService,
                clubGalleryRepository,
                currentUserService,
                auth,
                mapper
        );
        Role studentRole = new Role((short) 1, "STUDENT");
        Role leaderRole = new Role((short) 2, "CLUB_LEADER");
        Role adminRole = new Role((short) 3, "ADMIN");
        leader = new User(UUID.randomUUID(), "leader@example.com", "Leader", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(studentRole, leaderRole));
        otherLeader = new User(UUID.randomUUID(), "other@example.com", "Other", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(studentRole, leaderRole));
        admin = new User(UUID.randomUUID(), "admin@example.com", "Admin", null,
                LocalDateTime.now(), LocalDateTime.now(), Set.of(adminRole));
        club = new Club(10L, leader, "Programming Club", "Desc", null, null, null, null, null);
        otherClub = new Club(20L, otherLeader, "Robotics Club", "Desc", null, null, null, null, null);
    }

    @Test
    @DisplayName("Exact club leader can add gallery item")
    void exactLeaderCanAddGalleryItem() {
        ClubGalleryCreateRequest request = new ClubGalleryCreateRequest("https://example.com/image.jpg", " Caption ");
        ClubGallery saved = new ClubGallery(50L, club, "https://example.com/image.jpg", "Caption", null);
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(clubGalleryRepository.save(any(ClubGallery.class))).thenReturn(saved);

        ClubGalleryResponse response = galleryService.addGalleryItem(10L, request);

        assertThat(response.galleryId()).isEqualTo(50L);
        assertThat(response.caption()).isEqualTo("Caption");
    }

    @Test
    @DisplayName("ROLE_CLUB_LEADER alone cannot add to another leader's club")
    void otherLeaderCannotAddGalleryItem() {
        ClubGalleryCreateRequest request = new ClubGalleryCreateRequest("https://example.com/image.jpg", null);
        when(currentUserService.getCurrentUser()).thenReturn(otherLeader);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);

        assertThatThrownBy(() -> galleryService.addGalleryItem(10L, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(clubGalleryRepository, never()).save(any(ClubGallery.class));
    }

    @Test
    @DisplayName("Admin can delete gallery item")
    void adminCanDeleteGalleryItem() {
        ClubGallery gallery = new ClubGallery(50L, club, "https://example.com/image.jpg", null, null);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(clubGalleryRepository.findByIdWithClub(50L)).thenReturn(Optional.of(gallery));

        galleryService.deleteGalleryItem(10L, 50L);

        verify(clubGalleryRepository).delete(gallery);
    }

    @Test
    @DisplayName("Deleting gallery item from another club is rejected")
    void galleryItemFromAnotherClubIsRejected() {
        ClubGallery gallery = new ClubGallery(50L, otherClub, "https://example.com/image.jpg", null, null);
        when(currentUserService.getCurrentUser()).thenReturn(leader);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);
        when(clubGalleryRepository.findByIdWithClub(50L)).thenReturn(Optional.of(gallery));

        assertThatThrownBy(() -> galleryService.deleteGalleryItem(10L, 50L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(clubGalleryRepository, never()).delete(any(ClubGallery.class));
    }

    @Test
    @DisplayName("ROLE_CLUB_LEADER alone cannot delete another leader's gallery")
    void otherLeaderCannotDeleteGalleryItem() {
        when(currentUserService.getCurrentUser()).thenReturn(otherLeader);
        when(clubService.findClubWithLeader(10L)).thenReturn(club);

        assertThatThrownBy(() -> galleryService.deleteGalleryItem(10L, 50L))
                .isInstanceOf(AccessDeniedException.class);

        verify(clubGalleryRepository, never()).findByIdWithClub(any(Long.class));
        verify(clubGalleryRepository, never()).delete(any(ClubGallery.class));
    }
}
