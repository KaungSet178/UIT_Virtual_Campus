package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, EventRepositoryCustom {

    long countByEndDatetimeGreaterThanEqual(OffsetDateTime now);

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.createdBy creator
            LEFT JOIN FETCH e.club club
            LEFT JOIN FETCH club.leader leader
            WHERE e.eventId = :eventId
            """)
    Optional<Event> findByIdWithDetails(@Param("eventId") Long eventId);

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.createdBy creator
            LEFT JOIN FETCH e.club club
            LEFT JOIN FETCH club.leader leader
            WHERE e.endDatetime >= :now
            ORDER BY e.startDatetime ASC, e.eventId ASC
            """)
    List<Event> findUpcomingWithDetails(
            @Param("now") OffsetDateTime now
    );

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.createdBy creator
            JOIN FETCH e.club club
            LEFT JOIN FETCH club.leader leader
            WHERE e.endDatetime >= :now
              AND club.clubId = :clubId
            ORDER BY e.startDatetime ASC, e.eventId ASC
            """)
    List<Event> findUpcomingByClubWithDetails(
            @Param("now") OffsetDateTime now,
            @Param("clubId") Long clubId
    );

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.createdBy creator
            LEFT JOIN FETCH e.club club
            LEFT JOIN FETCH club.leader leader
            WHERE e.endDatetime >= :now
              AND LOWER(e.location) = LOWER(:location)
            ORDER BY e.startDatetime ASC, e.eventId ASC
            """)
    List<Event> findUpcomingByLocationWithDetails(
            @Param("now") OffsetDateTime now,
            @Param("location") String location
    );

    @Query("""
            SELECT e FROM Event e
            JOIN FETCH e.createdBy creator
            JOIN FETCH e.club club
            LEFT JOIN FETCH club.leader leader
            WHERE e.endDatetime >= :now
              AND club.clubId = :clubId
              AND LOWER(e.location) = LOWER(:location)
            ORDER BY e.startDatetime ASC, e.eventId ASC
            """)
    List<Event> findUpcomingByClubAndLocationWithDetails(
            @Param("now") OffsetDateTime now,
            @Param("clubId") Long clubId,
            @Param("location") String location
    );
}
