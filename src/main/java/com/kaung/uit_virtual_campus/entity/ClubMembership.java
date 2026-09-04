package com.kaung.uit_virtual_campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "club_memberships",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(columnNames = {"club_id", "student_id"})
)
public class ClubMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClubMembershipStatus status;

    protected ClubMembership() {
    }

    public ClubMembership(Long membershipId, Club club, StudentProfile student,
                          OffsetDateTime joinedAt, ClubMembershipStatus status) {
        this.membershipId = membershipId;
        this.club = club;
        this.student = student;
        this.joinedAt = joinedAt;
        this.status = status;
    }

    public ClubMembership(Club club, StudentProfile student) {
        this.club = club;
        this.student = student;
        this.status = ClubMembershipStatus.ACTIVE;
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public Club getClub() {
        return club;
    }

    public StudentProfile getStudent() {
        return student;
    }

    public OffsetDateTime getJoinedAt() {
        return joinedAt;
    }

    public ClubMembershipStatus getStatus() {
        return status;
    }

    public void activate() {
        this.status = ClubMembershipStatus.ACTIVE;
    }

    public void leave() {
        this.status = ClubMembershipStatus.LEFT;
    }

    @PrePersist
    void prePersist() {
        if (joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClubMembership that)) {
            return false;
        }
        return membershipId != null && Objects.equals(membershipId, that.membershipId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
