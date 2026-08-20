package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.ClubGallery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubGalleryRepository extends JpaRepository<ClubGallery, Long> {

    @Query("""
            SELECT g FROM ClubGallery g
            JOIN FETCH g.club c
            JOIN FETCH c.leader leader
            WHERE c.clubId = :clubId
            ORDER BY g.uploadedAt DESC, g.galleryId DESC
            """)
    List<ClubGallery> findByClubIdWithClub(@Param("clubId") Long clubId);

    @Query("""
            SELECT g FROM ClubGallery g
            JOIN FETCH g.club c
            JOIN FETCH c.leader leader
            WHERE g.galleryId = :galleryId
            """)
    Optional<ClubGallery> findByIdWithClub(@Param("galleryId") Long galleryId);
}
