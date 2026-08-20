package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ClubGalleryCreateRequest;
import com.kaung.uit_virtual_campus.dto.ClubGalleryResponse;
import com.kaung.uit_virtual_campus.entity.Club;
import com.kaung.uit_virtual_campus.entity.ClubGallery;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.ClubGalleryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClubGalleryService {

    private final ClubService clubService;
    private final ClubGalleryRepository clubGalleryRepository;
    private final CurrentUserService currentUserService;
    private final ClubAuthorizationService authorizationService;
    private final ClubMapper clubMapper;

    public ClubGalleryService(
            ClubService clubService,
            ClubGalleryRepository clubGalleryRepository,
            CurrentUserService currentUserService,
            ClubAuthorizationService authorizationService,
            ClubMapper clubMapper
    ) {
        this.clubService = clubService;
        this.clubGalleryRepository = clubGalleryRepository;
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.clubMapper = clubMapper;
    }

    public List<ClubGalleryResponse> getGallery(Long clubId) {
        clubService.findClubWithLeader(clubId);
        return clubGalleryRepository.findByClubIdWithClub(clubId).stream()
                .map(clubMapper::toGalleryResponse)
                .toList();
    }

    @Transactional
    public ClubGalleryResponse addGalleryItem(Long clubId, ClubGalleryCreateRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        Club club = clubService.findClubWithLeader(clubId);
        authorizationService.requireClubManager(currentUser, club);
        ClubGallery saved = clubGalleryRepository.save(new ClubGallery(
                club,
                request.imageUrl().trim(),
                trimToNull(request.caption())
        ));
        return clubMapper.toGalleryResponse(saved);
    }

    @Transactional
    public void deleteGalleryItem(Long clubId, Long galleryId) {
        User currentUser = currentUserService.getCurrentUser();
        Club club = clubService.findClubWithLeader(clubId);
        authorizationService.requireClubManager(currentUser, club);
        ClubGallery gallery = clubGalleryRepository.findByIdWithClub(galleryId)
                .orElseThrow(() -> new ResourceNotFoundException("Club gallery item not found with id: " + galleryId));
        if (!gallery.getClub().getClubId().equals(clubId)) {
            throw new ResourceNotFoundException("Club gallery item not found with id: " + galleryId);
        }
        clubGalleryRepository.delete(gallery);
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
