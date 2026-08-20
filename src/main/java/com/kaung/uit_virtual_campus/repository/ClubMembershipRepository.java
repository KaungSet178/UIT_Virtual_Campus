package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.ClubMembership;
import com.kaung.uit_virtual_campus.entity.ClubMembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClubMembershipRepository extends JpaRepository<ClubMembership, Long> {

    interface ClubMemberCount {
        Long getClubId();

        long getActiveMemberCount();
    }

    @Query("""
            SELECT m FROM ClubMembership m
            JOIN FETCH m.club c
            JOIN FETCH c.leader leader
            JOIN FETCH m.student sp
            JOIN FETCH sp.user u
            WHERE c.clubId = :clubId
              AND sp.studentId = :studentId
            """)
    Optional<ClubMembership> findByClubIdAndStudentIdWithDetails(
            @Param("clubId") Long clubId,
            @Param("studentId") UUID studentId
    );

    @Query("""
            SELECT m FROM ClubMembership m
            JOIN FETCH m.club c
            JOIN FETCH c.leader leader
            JOIN FETCH m.student sp
            JOIN FETCH sp.user u
            WHERE c.clubId = :clubId
              AND m.status = :status
            ORDER BY u.fullName ASC
            """)
    List<ClubMembership> findByClubIdAndStatusWithDetails(
            @Param("clubId") Long clubId,
            @Param("status") ClubMembershipStatus status
    );

    @Query("""
            SELECT m FROM ClubMembership m
            JOIN FETCH m.club c
            JOIN FETCH c.leader leader
            JOIN FETCH m.student sp
            JOIN FETCH sp.user u
            WHERE sp.studentId = :studentId
            ORDER BY c.clubName ASC
            """)
    List<ClubMembership> findByStudentIdWithDetails(@Param("studentId") UUID studentId);

    @Query("""
            SELECT m.club.clubId AS clubId, COUNT(m) AS activeMemberCount
            FROM ClubMembership m
            WHERE m.club.clubId IN :clubIds
              AND m.status = :status
            GROUP BY m.club.clubId
            """)
    List<ClubMemberCount> countByClubIdsAndStatus(
            @Param("clubIds") List<Long> clubIds,
            @Param("status") ClubMembershipStatus status
    );

    long countByClubClubIdAndStatus(Long clubId, ClubMembershipStatus status);
}
