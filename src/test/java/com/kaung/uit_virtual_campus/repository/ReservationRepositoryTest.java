package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Reservation;
import com.kaung.uit_virtual_campus.entity.ReservationStatus;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReservationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ReservationRepository reservationRepository;

    private User user;
    private Room room;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        Building building = entityManager.persist(new Building("Building 1", "Main"));
        Floor floor = entityManager.persist(new Floor(building, 1));
        room = entityManager.persist(new Room(floor, "111", "Room 111", "CLASSROOM", "Room", "room_111"));
        user = entityManager.persist(new User(
                UUID.randomUUID(),
                "user@example.com",
                "User",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of()
        ));
        date = LocalDate.of(2026, 8, 24);

        Reservation approved = new Reservation(user, room, date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Approved");
        approved.approve(user);
        entityManager.persist(approved);

        entityManager.persist(new Reservation(user, room, date, LocalTime.of(11, 0), LocalTime.of(12, 0), "Pending"));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Approved overlap predicate detects overlapping approved reservation")
    void detectsApprovedOverlap() {
        long overlaps = reservationRepository.countApprovedOverlappingReservations(
                room.getRoomId(),
                date,
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                null
        );

        assertThat(overlaps).isEqualTo(1);
    }

    @Test
    @DisplayName("Approved overlap predicate allows back-to-back reservations")
    void allowsBackToBackApprovedReservations() {
        long overlaps = reservationRepository.countApprovedOverlappingReservations(
                room.getRoomId(),
                date,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null
        );

        assertThat(overlaps).isZero();
    }

    @Test
    @DisplayName("Pending reservations do not block availability")
    void pendingReservationsDoNotBlock() {
        long overlaps = reservationRepository.countApprovedOverlappingReservations(
                room.getRoomId(),
                date,
                LocalTime.of(11, 30),
                LocalTime.of(12, 30),
                null
        );

        assertThat(overlaps).isZero();
    }

    @Test
    @DisplayName("Finds reservations for current user with details")
    void findsUserReservationsWithDetails() {
        List<Reservation> reservations = reservationRepository.findByUserIdWithDetails(user.getUserId());

        assertThat(reservations).hasSize(2);
        assertThat(reservations.get(0).getUser().getEmail()).isEqualTo("user@example.com");
        assertThat(reservations.get(0).getRoom().getFloor().getBuilding().getBuildingName()).isEqualTo("Building 1");
    }

    @Test
    @DisplayName("Finds reservation by id with requester and room details")
    void findsReservationByIdWithDetails() {
        Reservation first = reservationRepository.findByUserIdWithDetails(user.getUserId()).get(0);

        Optional<Reservation> found = reservationRepository.findByIdWithDetails(first.getReservationId());

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("user@example.com");
        assertThat(found.get().getRoom().getRoomNumber()).isEqualTo("111");
    }

    @Test
    @DisplayName("Creating reservation populates audit timestamps")
    void createPopulatesAuditTimestamps() {
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(
                user,
                room,
                LocalDate.of(2026, 9, 4),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                "meeting"
        ));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getCreatedAt()).isNotNull();
        assertThat(reservation.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Approving reservation refreshes updatedAt")
    void approveRefreshesUpdatedAt() {
        OffsetDateTime oldTimestamp = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(
                null,
                user,
                room,
                LocalDate.of(2026, 9, 4),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                "meeting",
                ReservationStatus.PENDING,
                null,
                oldTimestamp,
                oldTimestamp
        ));

        reservation.approve(user);
        entityManager.flush();

        assertThat(reservation.getCreatedAt()).isEqualTo(oldTimestamp);
        assertThat(reservation.getUpdatedAt()).isAfter(oldTimestamp);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    @DisplayName("Rejecting reservation refreshes updatedAt")
    void rejectRefreshesUpdatedAt() {
        OffsetDateTime oldTimestamp = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(
                null,
                user,
                room,
                LocalDate.of(2026, 9, 4),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                "meeting",
                ReservationStatus.PENDING,
                null,
                oldTimestamp,
                oldTimestamp
        ));

        reservation.reject();
        entityManager.flush();

        assertThat(reservation.getCreatedAt()).isEqualTo(oldTimestamp);
        assertThat(reservation.getUpdatedAt()).isAfter(oldTimestamp);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.REJECTED);
    }

    @Test
    @DisplayName("Cancelling reservation refreshes updatedAt")
    void cancelRefreshesUpdatedAt() {
        OffsetDateTime oldTimestamp = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(
                null,
                user,
                room,
                LocalDate.of(2026, 9, 4),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                "meeting",
                ReservationStatus.PENDING,
                null,
                oldTimestamp,
                oldTimestamp
        ));

        reservation.cancel();
        entityManager.flush();

        assertThat(reservation.getCreatedAt()).isEqualTo(oldTimestamp);
        assertThat(reservation.getUpdatedAt()).isAfter(oldTimestamp);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
}
