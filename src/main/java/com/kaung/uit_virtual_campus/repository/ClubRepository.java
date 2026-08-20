package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {

    @Query("""
            SELECT c FROM Club c
            JOIN FETCH c.leader leader
            ORDER BY c.clubName ASC
            """)
    List<Club> findAllWithLeader();

    @Query("""
            SELECT c FROM Club c
            JOIN FETCH c.leader leader
            WHERE c.clubId = :clubId
            """)
    Optional<Club> findByIdWithLeader(@Param("clubId") Long clubId);

    List<Club> findByLeaderUserIdOrderByClubNameAsc(UUID leaderId);

    boolean existsByClubNameIgnoreCase(String clubName);
}
