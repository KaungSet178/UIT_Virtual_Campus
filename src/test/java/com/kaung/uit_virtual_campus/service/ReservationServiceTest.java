package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ReservationCreateRequest;
import com.kaung.uit_virtual_campus.dto.ReservationResponse;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Reservation;
import com.kaung.uit_virtual_campus.entity.ReservationStatus;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.repository.ReservationRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private RoomAvailabilityService availabilityService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    private ReservationService reservationService;
    private User user;
    private User admin;
    private Room room;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                roomRepository,
                currentUserService,
                availabilityService,
                notificationService,
                userRepository
        );
        user = new User(UUID.randomUUID(), "user@example.com", "User", null, LocalDateTime.now(), LocalDateTime.now(), Set.of());
        admin = new User(UUID.randomUUID(), "admin@example.com", "Admin", null, LocalDateTime.now(), LocalDateTime.now(), Set.of(new Role((short) 3, "ADMIN")));
        Building building = new Building(1L, "Building 1", "Main");
        Floor floor = new Floor(101L, building, 1);
        room = new Room(10L, floor, "111", "Room 111", "CLASSROOM", "Room", "room_111");
        reservation = new Reservation(30L, user, room, LocalDate.of(2026, 8, 24),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting", ReservationStatus.PENDING,
                null, null, null);
    }

    @Test
    @DisplayName("Creates pending reservation with room lock")
    void createReservationUsesRoomLock() {
        ReservationCreateRequest request = new ReservationCreateRequest(
                10L,
                LocalDate.of(2026, 8, 24),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                " Meeting "
        );
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(reservationRepository.findByIdWithDetails(30L)).thenReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.createReservation(request);

        assertThat(response.status()).isEqualTo(ReservationStatus.PENDING);
        verify(roomRepository).findByIdForUpdate(10L);
        verify(availabilityService).assertAvailableForReservation(
                10L,
                LocalDate.of(2026, 8, 24),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null
        );
    }

    @Test
    @DisplayName("Create reservation rejects the same scheduled slot that availability rejects")
    void createReservationRejectsUnavailableScheduledSlot() {
        ReservationCreateRequest request = new ReservationCreateRequest(
                10L,
                LocalDate.of(2026, 9, 7),
                LocalTime.of(8, 30),
                LocalTime.of(9, 30),
                " Meeting "
        );
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));
        org.mockito.Mockito.doThrow(new ConflictException("Room is occupied by an official schedule during the requested time."))
                .when(availabilityService)
                .assertAvailableForReservation(
                        10L,
                        LocalDate.of(2026, 9, 7),
                        LocalTime.of(8, 30),
                        LocalTime.of(9, 30),
                        null
                );

        assertThatThrownBy(() -> reservationService.createReservation(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("official schedule");
    }

    @Test
    @DisplayName("Approves pending reservation with room lock and approver retained")
    void approveReservation() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(reservationRepository.findByIdWithDetails(30L)).thenReturn(Optional.of(reservation));
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));

        ReservationResponse response = reservationService.approveReservation(30L);

        assertThat(response.status()).isEqualTo(ReservationStatus.APPROVED);
        assertThat(response.approvedByUserId()).isEqualTo(admin.getUserId());
        verify(roomRepository).findByIdForUpdate(10L);
    }

    @Test
    @DisplayName("Rejects invalid approve transition")
    void rejectInvalidApproveTransition() {
        reservation.cancel();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(reservationRepository.findByIdWithDetails(30L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.approveReservation(30L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only pending reservations can be approved");
    }

    @Test
    @DisplayName("Cancelling approved reservation keeps approvedBy history")
    void cancelApprovedKeepsApprover() {
        reservation.approve(admin);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(reservationRepository.findByIdWithDetails(30L)).thenReturn(Optional.of(reservation));
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));

        ReservationResponse response = reservationService.cancelReservation(30L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(response.approvedByUserId()).isEqualTo(admin.getUserId());
    }

    @Test
    @DisplayName("Rejects access to another user's reservation")
    void rejectsOtherUserAccess() {
        User other = new User(UUID.randomUUID(), "other@example.com", "Other", null, LocalDateTime.now(), LocalDateTime.now(), Set.of());
        when(currentUserService.getCurrentUser()).thenReturn(other);
        when(reservationRepository.findByIdWithDetails(30L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationByIdForCurrentUser(30L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
