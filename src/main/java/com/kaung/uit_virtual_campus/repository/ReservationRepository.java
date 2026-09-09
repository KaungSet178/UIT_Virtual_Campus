package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Reservation;
import com.kaung.uit_virtual_campus.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    long countByStatus(ReservationStatus status);

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.user u
            JOIN FETCH r.room room
            JOIN FETCH room.floor f
            JOIN FETCH f.building b
            LEFT JOIN FETCH r.approvedBy approver
            WHERE r.reservationId = :reservationId
            """)
    Optional<Reservation> findByIdWithDetails(@Param("reservationId") Long reservationId);

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.user u
            JOIN FETCH r.room room
            JOIN FETCH room.floor f
            JOIN FETCH f.building b
            LEFT JOIN FETCH r.approvedBy approver
            WHERE u.userId = :userId
            ORDER BY r.reservationDate DESC, r.startTime ASC, r.createdAt DESC
            """)
    List<Reservation> findByUserIdWithDetails(@Param("userId") UUID userId);

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.user u
            JOIN FETCH r.room room
            JOIN FETCH room.floor f
            JOIN FETCH f.building b
            LEFT JOIN FETCH r.approvedBy approver
            WHERE (:status IS NULL OR r.status = :status)
              AND (:roomId IS NULL OR room.roomId = :roomId)
              AND (:userId IS NULL OR u.userId = :userId)
              AND (:reservationDate IS NULL OR r.reservationDate = :reservationDate)
            ORDER BY r.createdAt DESC, r.reservationDate DESC, r.startTime ASC
            """)
    List<Reservation> findForAdmin(
            @Param("status") ReservationStatus status,
            @Param("roomId") Long roomId,
            @Param("userId") UUID userId,
            @Param("reservationDate") LocalDate reservationDate
    );

    @Query("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.room.roomId = :roomId
              AND r.reservationDate = :reservationDate
              AND r.status = com.kaung.uit_virtual_campus.entity.ReservationStatus.APPROVED
              AND r.startTime < :requestedEnd
              AND r.endTime > :requestedStart
              AND (:excludedReservationId IS NULL OR r.reservationId <> :excludedReservationId)
            """)
    long countApprovedOverlappingReservations(
            @Param("roomId") Long roomId,
            @Param("reservationDate") LocalDate reservationDate,
            @Param("requestedStart") LocalTime requestedStart,
            @Param("requestedEnd") LocalTime requestedEnd,
            @Param("excludedReservationId") Long excludedReservationId
    );

    @Query("""
            SELECT r FROM Reservation r
            JOIN FETCH r.room room
            WHERE room.roomId = :roomId
              AND r.reservationDate = :reservationDate
              AND r.status = com.kaung.uit_virtual_campus.entity.ReservationStatus.APPROVED
              AND r.startTime < :requestedEnd
              AND r.endTime > :requestedStart
            ORDER BY r.startTime ASC
            """)
    List<Reservation> findApprovedOverlappingReservations(
            @Param("roomId") Long roomId,
            @Param("reservationDate") LocalDate reservationDate,
            @Param("requestedStart") LocalTime requestedStart,
            @Param("requestedEnd") LocalTime requestedEnd
    );

    // Same predicate as findApprovedOverlappingReservations minus the roomId
    // filter, for the bulk "which rooms are occupied right now" check.
    @Query("""
            SELECT DISTINCT r.room.roomId FROM Reservation r
            WHERE r.reservationDate = :reservationDate
              AND r.status = com.kaung.uit_virtual_campus.entity.ReservationStatus.APPROVED
              AND r.startTime < :requestedEnd
              AND r.endTime > :requestedStart
            """)
    List<Long> findOccupiedRoomIdsByReservation(
            @Param("reservationDate") LocalDate reservationDate,
            @Param("requestedStart") LocalTime requestedStart,
            @Param("requestedEnd") LocalTime requestedEnd
    );
}
