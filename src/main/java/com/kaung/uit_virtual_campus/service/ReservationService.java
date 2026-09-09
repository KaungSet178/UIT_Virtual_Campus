package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ReservationCreateRequest;
import com.kaung.uit_virtual_campus.dto.ReservationResponse;
import com.kaung.uit_virtual_campus.entity.NotificationEntityType;
import com.kaung.uit_virtual_campus.entity.NotificationType;
import com.kaung.uit_virtual_campus.entity.Reservation;
import com.kaung.uit_virtual_campus.entity.ReservationStatus;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.ReservationRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final CurrentUserService currentUserService;
    private final RoomAvailabilityService availabilityService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public ReservationService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            CurrentUserService currentUserService,
            RoomAvailabilityService availabilityService,
            NotificationService notificationService,
            UserRepository userRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.currentUserService = currentUserService;
        this.availabilityService = availabilityService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request) {
        User user = currentUserService.getCurrentUser();
        Room room = lockRoom(request.roomId());
        availabilityService.validateDateTime(request.reservationDate(), request.startTime(), request.endTime());
        availabilityService.assertAvailableForReservation(
                room.getRoomId(),
                request.reservationDate(),
                request.startTime(),
                request.endTime(),
                null
        );

        Reservation saved = reservationRepository.save(new Reservation(
                user,
                room,
                request.reservationDate(),
                request.startTime(),
                request.endTime(),
                request.purpose().trim()
        ));
        notifyAdmins(
                NotificationType.RESERVATION_SUBMITTED,
                "New Reservation Request",
                displayName(user) + " requested " + roomLabel(room) + " for " + reservationSlot(saved) + ".",
                saved.getReservationId()
        );
        return getReservationByIdForCurrentUser(saved.getReservationId());
    }

    public List<ReservationResponse> getCurrentUserReservations() {
        UUID userId = currentUserService.getCurrentUserId();
        return reservationRepository.findByUserIdWithDetails(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ReservationResponse getReservationByIdForCurrentUser(Long reservationId) {
        User currentUser = currentUserService.getCurrentUser();
        Reservation reservation = findWithDetails(reservationId);
        if (!isOwner(reservation, currentUser) && !hasRole(currentUser, "ADMIN")) {
            throw new AccessDeniedException("Access denied");
        }
        return toResponse(reservation);
    }

    public List<ReservationResponse> getReservationsForAdmin(
            ReservationStatus status,
            Long roomId,
            UUID userId,
            LocalDate reservationDate
    ) {
        return reservationRepository.findForAdmin(status, roomId, userId, reservationDate).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ReservationResponse approveReservation(Long reservationId) {
        User admin = currentUserService.getCurrentUser();
        Reservation reservation = findWithDetails(reservationId);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ConflictException("Only pending reservations can be approved.");
        }

        lockRoom(reservation.getRoom().getRoomId());
        availabilityService.assertAvailableForReservation(
                reservation.getRoom().getRoomId(),
                reservation.getReservationDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getReservationId()
        );
        reservation.approve(admin);
        notificationService.notifyUser(
                reservation.getUser().getUserId(),
                NotificationType.RESERVATION_APPROVED,
                "Reservation Approved",
                "Your reservation for " + roomLabel(reservation.getRoom()) + " on " + reservationSlot(reservation)
                        + " has been approved.",
                NotificationEntityType.RESERVATION,
                reservation.getReservationId()
        );
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse rejectReservation(Long reservationId) {
        Reservation reservation = findWithDetails(reservationId);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new ConflictException("Only pending reservations can be rejected.");
        }
        reservation.reject();
        notificationService.notifyUser(
                reservation.getUser().getUserId(),
                NotificationType.RESERVATION_REJECTED,
                "Reservation Rejected",
                "Your reservation for " + roomLabel(reservation.getRoom()) + " on " + reservationSlot(reservation)
                        + " has been rejected.",
                NotificationEntityType.RESERVATION,
                reservation.getReservationId()
        );
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId) {
        User currentUser = currentUserService.getCurrentUser();
        Reservation reservation = findWithDetails(reservationId);
        if (!isOwner(reservation, currentUser) && !hasRole(currentUser, "ADMIN")) {
            throw new AccessDeniedException("Access denied");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.APPROVED) {
            throw new ConflictException("Reservation cannot be cancelled from status: " + reservation.getStatus() + ".");
        }
        if (reservation.getStatus() == ReservationStatus.APPROVED) {
            lockRoom(reservation.getRoom().getRoomId());
        }
        reservation.cancel();
        notifyAdmins(
                NotificationType.RESERVATION_CANCELLED,
                "Reservation Cancelled",
                displayName(reservation.getUser()) + " cancelled " + roomLabel(reservation.getRoom())
                        + " for " + reservationSlot(reservation) + ".",
                reservation.getReservationId()
        );
        return toResponse(reservation);
    }

    private Reservation findWithDetails(Long reservationId) {
        return reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
    }

    private boolean isOwner(Reservation reservation, User currentUser) {
        return reservation.getUser().getUserId().equals(currentUser.getUserId());
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getRoleName().equalsIgnoreCase(roleName));
    }

    private void notifyAdmins(NotificationType type, String title, String message, Long reservationId) {
        notificationService.notifyUsers(
                userRepository.findUserIdsByRoleName("ADMIN"),
                type,
                title,
                message,
                NotificationEntityType.RESERVATION,
                reservationId
        );
    }

    private String reservationSlot(Reservation reservation) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return reservation.getReservationDate().format(dateFormatter) + ", "
                + reservation.getStartTime().format(timeFormatter) + "-"
                + reservation.getEndTime().format(timeFormatter);
    }

    private String roomLabel(Room room) {
        if (room.getRoomName() == null || room.getRoomName().isBlank()) {
            return "Room " + room.getRoomNumber();
        }
        return room.getRoomName();
    }

    private String displayName(User user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
    }

    private ReservationResponse toResponse(Reservation reservation) {
        User approver = reservation.getApprovedBy();
        return new ReservationResponse(
                reservation.getReservationId(),
                reservation.getUser().getUserId(),
                reservation.getUser().getEmail(),
                reservation.getUser().getFullName(),
                reservation.getRoom().getRoomId(),
                reservation.getRoom().getRoomNumber(),
                reservation.getRoom().getRoomName(),
                reservation.getRoom().getFloor().getBuilding().getBuildingId(),
                reservation.getRoom().getFloor().getBuilding().getBuildingName(),
                reservation.getRoom().getFloor().getFloorId(),
                reservation.getRoom().getFloor().getFloorNumber(),
                reservation.getReservationDate(),
                reservation.getStartTime(),
                reservation.getEndTime(),
                reservation.getPurpose(),
                reservation.getStatus(),
                approver == null ? null : approver.getUserId(),
                approver == null ? null : approver.getEmail(),
                approver == null ? null : approver.getFullName(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
