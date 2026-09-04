package com.kaung.uit_virtual_campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "club_galleries", schema = "public")
public class ClubGallery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gallery_id", nullable = false)
    private Long galleryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "caption")
    private String caption;

    @Column(name = "uploaded_at")
    private OffsetDateTime uploadedAt;

    protected ClubGallery() {
    }

    public ClubGallery(Long galleryId, Club club, String imageUrl, String caption, OffsetDateTime uploadedAt) {
        this.galleryId = galleryId;
        this.club = club;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.uploadedAt = uploadedAt;
    }

    public ClubGallery(Club club, String imageUrl, String caption) {
        this.club = club;
        this.imageUrl = imageUrl;
        this.caption = caption;
    }

    public Long getGalleryId() {
        return galleryId;
    }

    public Club getClub() {
        return club;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCaption() {
        return caption;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = OffsetDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClubGallery that)) {
            return false;
        }
        return galleryId != null && Objects.equals(galleryId, that.galleryId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
