package com.kaung.uit_virtual_campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "clubs", schema = "public")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_id", nullable = false)
    private User leader;

    @Column(name = "club_name", nullable = false, unique = true)
    private String clubName;

    @Column(name = "description")
    private String description;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "club", fetch = FetchType.LAZY)
    @OrderBy("joinedAt DESC")
    private List<ClubMembership> memberships = new ArrayList<>();

    @OneToMany(mappedBy = "club", fetch = FetchType.LAZY)
    @OrderBy("uploadedAt DESC, galleryId DESC")
    private List<ClubGallery> galleryItems = new ArrayList<>();

    protected Club() {
    }

    public Club(Long clubId, User leader, String clubName, String description,
                String contactEmail, String contactPhone, String logoUrl,
                OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.clubId = clubId;
        this.leader = leader;
        this.clubName = clubName;
        this.description = description;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.logoUrl = logoUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Club(User leader, String clubName, String description,
                String contactEmail, String contactPhone, String logoUrl) {
        this.leader = leader;
        this.clubName = clubName;
        this.description = description;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.logoUrl = logoUrl;
    }

    public Long getClubId() {
        return clubId;
    }

    public User getLeader() {
        return leader;
    }

    public String getClubName() {
        return clubName;
    }

    public String getDescription() {
        return description;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ClubMembership> getMemberships() {
        return Collections.unmodifiableList(memberships);
    }

    public List<ClubGallery> getGalleryItems() {
        return Collections.unmodifiableList(galleryItems);
    }

    public void updateProfile(String clubName, String description, String contactEmail,
                              String contactPhone, String logoUrl) {
        this.clubName = clubName;
        this.description = description;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.logoUrl = logoUrl;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Club club)) {
            return false;
        }
        return clubId != null && Objects.equals(clubId, club.clubId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
