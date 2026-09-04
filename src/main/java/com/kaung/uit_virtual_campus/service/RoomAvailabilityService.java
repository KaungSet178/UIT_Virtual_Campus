package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.AvailabilityConflictResponse;
import com.kaung.uit_virtual_campus.dto.RoomAvailabilityResponse;
import com.kaung.uit_virtual_campus.entity.Reservation;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.ReservationRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoomAvailabilityService {

    private final RoomRepository roomRepository;
    private final ScheduleRepository scheduleRepository;
    private final ReservationRepository reservationRepository;

    public RoomAvailabilityService(
            RoomRepository roomRepository,
            ScheduleRepository scheduleRepository,
            ReservationRepository reservationRepository
    ) {
        this.roomRepository = roomRepository;
        this.scheduleRepository = scheduleRepository;
        this.reservationRepository = reservationRepository;
    }

    public RoomAvailabilityResponse getAvailability(
            Long roomId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        validateDateTime(date, startTime, endTime);
        if (!roomRepository.existsById(roomId)) {
            throw new ResourceNotFoundException("Room not found with id: " + roomId);
        }

        List<AvailabilityConflictResponse> conflicts = new ArrayList<>();
        conflicts.addAll(findOfficialScheduleConflicts(roomId, date, startTime, endTime));
        conflicts.addAll(findApprovedReservationConflicts(roomId, date, startTime, endTime));

        return new RoomAvailabilityResponse(
                roomId,
                date,
                startTime,
                endTime,
                conflicts.isEmpty(),
                conflicts
        );
    }

    public void assertAvailableForReservation(
            Long roomId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            Long excludedReservationId
    ) {
        validateDateTime(date, startTime, endTime);
        List<AvailabilityConflictResponse> scheduleConflicts = findOfficialScheduleConflicts(roomId, date, startTime, endTime);
        if (!scheduleConflicts.isEmpty()) {
            throw new ConflictException("Room is occupied by an official schedule during the requested time.");
        }

        long reservationConflicts = reservationRepository.countApprovedOverlappingReservations(
                roomId,
                date,
                startTime,
                endTime,
                excludedReservationId
        );
        if (reservationConflicts > 0) {
            throw new ConflictException("Room already has an approved reservation during the requested time.");
        }
    }

    public void validateDateTime(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date == null) {
            throw new IllegalArgumentException("date is required.");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime.");
        }
    }

    private List<AvailabilityConflictResponse> findOfficialScheduleConflicts(
            Long roomId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return scheduleRepository.findOfficialScheduleConflicts(
                        roomId,
                        dayOfWeek(date),
                        date,
                        startTime,
                        endTime
                ).stream()
                .map(this::toScheduleConflict)
                .toList();
    }

    private List<AvailabilityConflictResponse> findApprovedReservationConflicts(
            Long roomId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime
    ) {
        return reservationRepository.findApprovedOverlappingReservations(roomId, date, startTime, endTime).stream()
                .map(this::toReservationConflict)
                .toList();
    }

    private AvailabilityConflictResponse toScheduleConflict(Schedule schedule) {
        return new AvailabilityConflictResponse(
                "SCHEDULE",
                schedule.getStartTime(),
                schedule.getEndTime(),
                "Official schedule: " + schedule.getCourseOffering().getSubject().getSubjectCode()
                        + " for " + schedule.getCourseOffering().getAcademicGroup().getDisplayName()
        );
    }

    private AvailabilityConflictResponse toReservationConflict(Reservation reservation) {
        return new AvailabilityConflictResponse(
                "APPROVED_RESERVATION",
                reservation.getStartTime(),
                reservation.getEndTime(),
                "Approved reservation"
        );
    }

    private Short dayOfWeek(LocalDate date) {
        return (short) date.getDayOfWeek().getValue();
    }
}
