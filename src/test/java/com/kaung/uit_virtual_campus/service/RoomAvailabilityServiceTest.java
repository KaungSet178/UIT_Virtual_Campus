package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.RoomAvailabilityResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Reservation;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.Subject;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.repository.ReservationRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomAvailabilityServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ReservationRepository reservationRepository;

    private RoomAvailabilityService availabilityService;
    private LocalDate date;
    private LocalTime start;
    private LocalTime end;

    @BeforeEach
    void setUp() {
        availabilityService =
                new RoomAvailabilityService(roomRepository, scheduleRepository, reservationRepository, Clock.systemUTC());
        date = LocalDate.of(2026, 8, 24);
        start = LocalTime.of(9, 30);
        end = LocalTime.of(10, 30);
    }

    @Test
    @DisplayName("Availability response includes the reservation purpose but redacts the owner's identity")
    void availabilityIncludesPurposeButRedactsOwner() {
        Reservation reservation = approvedReservation();
        when(roomRepository.existsById(10L)).thenReturn(true);
        when(scheduleRepository.findOfficialScheduleConflicts(10L, (short) 1, date, start, end)).thenReturn(List.of());
        when(reservationRepository.findApprovedOverlappingReservations(10L, date, start, end)).thenReturn(List.of(reservation));

        RoomAvailabilityResponse response = availabilityService.getAvailability(10L, date, start, end);

        assertThat(response.available()).isFalse();
        assertThat(response.conflicts()).hasSize(1);
        assertThat(response.conflicts().get(0).type()).isEqualTo("APPROVED_RESERVATION");
        // Purpose is deliberately surfaced (see feature_map_room_filter memory —
        // the map's Info panel shows *why* a room is occupied), but who booked
        // it stays private: no owner name or email in the description.
        assertThat(response.conflicts().get(0).description()).isEqualTo("Reservation: Secret purpose");
        assertThat(response.conflicts().get(0).description()).doesNotContain("owner@example.com", "Secret Owner");
    }

    @Test
    @DisplayName("Availability response includes non-sensitive schedule context")
    void availabilityIncludesScheduleContext() {
        Schedule schedule = schedule();
        when(roomRepository.existsById(10L)).thenReturn(true);
        when(scheduleRepository.findOfficialScheduleConflicts(10L, (short) 1, date, start, end)).thenReturn(List.of(schedule));
        when(reservationRepository.findApprovedOverlappingReservations(10L, date, start, end)).thenReturn(List.of());

        RoomAvailabilityResponse response = availabilityService.getAvailability(10L, date, start, end);

        assertThat(response.available()).isFalse();
        assertThat(response.conflicts().get(0).type()).isEqualTo("SCHEDULE");
        assertThat(response.conflicts().get(0).description()).contains("CS101", "Year 1 Section A");
    }

    @Test
    @DisplayName("Availability and reservation validation both block an official Monday class")
    void availabilityAndReservationValidationBothBlockOfficialSchedule() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        LocalTime periodStart = LocalTime.of(8, 30);
        LocalTime periodEnd = LocalTime.of(9, 30);
        Schedule schedule = new Schedule(
                20L,
                schedule().getCourseOffering(),
                schedule().getRoom(),
                (short) 1,
                periodStart,
                periodEnd
        );
        when(roomRepository.existsById(10L)).thenReturn(true);
        when(scheduleRepository.findOfficialScheduleConflicts(10L, (short) 1, monday, periodStart, periodEnd))
                .thenReturn(List.of(schedule));
        when(reservationRepository.findApprovedOverlappingReservations(10L, monday, periodStart, periodEnd))
                .thenReturn(List.of());

        RoomAvailabilityResponse availability = availabilityService.getAvailability(10L, monday, periodStart, periodEnd);

        assertThat(availability.available()).isFalse();
        assertThatThrownBy(() -> availabilityService.assertAvailableForReservation(
                10L,
                monday,
                periodStart,
                periodEnd,
                null
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("official schedule");
    }

    @Test
    @DisplayName("Back-to-back slot after class is available and valid for reservation")
    void backToBackSlotIsAvailableAndValidForReservation() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        LocalTime periodStart = LocalTime.of(9, 30);
        LocalTime periodEnd = LocalTime.of(9, 40);
        when(roomRepository.existsById(10L)).thenReturn(true);
        when(scheduleRepository.findOfficialScheduleConflicts(10L, (short) 1, monday, periodStart, periodEnd))
                .thenReturn(List.of());
        when(reservationRepository.findApprovedOverlappingReservations(10L, monday, periodStart, periodEnd))
                .thenReturn(List.of());
        when(reservationRepository.countApprovedOverlappingReservations(10L, monday, periodStart, periodEnd, null))
                .thenReturn(0L);

        RoomAvailabilityResponse availability = availabilityService.getAvailability(10L, monday, periodStart, periodEnd);

        assertThat(availability.available()).isTrue();
        availabilityService.assertAvailableForReservation(10L, monday, periodStart, periodEnd, null);
    }

    @Test
    @DisplayName("Class on Monday does not block Tuesday at the same local wall-clock time")
    void mondayClassDoesNotBlockTuesdaySameTime() {
        LocalDate tuesday = LocalDate.of(2026, 9, 8);
        LocalTime periodStart = LocalTime.of(8, 30);
        LocalTime periodEnd = LocalTime.of(9, 30);
        when(roomRepository.existsById(10L)).thenReturn(true);
        when(scheduleRepository.findOfficialScheduleConflicts(10L, (short) 2, tuesday, periodStart, periodEnd))
                .thenReturn(List.of());
        when(reservationRepository.findApprovedOverlappingReservations(10L, tuesday, periodStart, periodEnd))
                .thenReturn(List.of());
        when(reservationRepository.countApprovedOverlappingReservations(10L, tuesday, periodStart, periodEnd, null))
                .thenReturn(0L);

        RoomAvailabilityResponse availability = availabilityService.getAvailability(10L, tuesday, periodStart, periodEnd);

        assertThat(availability.available()).isTrue();
        availabilityService.assertAvailableForReservation(10L, tuesday, periodStart, periodEnd, null);
    }

    @Test
    @DisplayName("Approved reservations affect availability and reservation validation consistently")
    void approvedReservationsAffectBothConsistently() {
        Reservation reservation = approvedReservation();
        when(roomRepository.existsById(10L)).thenReturn(true);
        when(scheduleRepository.findOfficialScheduleConflicts(10L, (short) 1, date, start, end)).thenReturn(List.of());
        when(reservationRepository.findApprovedOverlappingReservations(10L, date, start, end)).thenReturn(List.of(reservation));
        when(reservationRepository.countApprovedOverlappingReservations(10L, date, start, end, null)).thenReturn(1L);

        RoomAvailabilityResponse availability = availabilityService.getAvailability(10L, date, start, end);

        assertThat(availability.available()).isFalse();
        assertThatThrownBy(() -> availabilityService.assertAvailableForReservation(10L, date, start, end, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("approved reservation");
    }

    @Test
    @DisplayName("Pending reservations remain non-blocking for availability and reservation validation")
    void pendingReservationsRemainNonBlockingForBoth() {
        when(roomRepository.existsById(10L)).thenReturn(true);
        when(scheduleRepository.findOfficialScheduleConflicts(10L, (short) 1, date, start, end)).thenReturn(List.of());
        when(reservationRepository.findApprovedOverlappingReservations(10L, date, start, end)).thenReturn(List.of());
        when(reservationRepository.countApprovedOverlappingReservations(10L, date, start, end, null)).thenReturn(0L);

        RoomAvailabilityResponse availability = availabilityService.getAvailability(10L, date, start, end);

        assertThat(availability.available()).isTrue();
        availabilityService.assertAvailableForReservation(10L, date, start, end, null);
    }

    @Test
    @DisplayName("Joint lecture schedules still make room occupied")
    void jointLectureSchedulesMakeRoomOccupied() {
        Schedule sectionA = schedule();
        AcademicGroup sectionBGroup = new AcademicGroup(5L, (short) 1, AcademicGroupType.SECTION, "B", null, null);
        CourseOffering sectionBOffering = new CourseOffering(
                6L,
                sectionA.getCourseOffering().getSemester(),
                sectionBGroup,
                sectionA.getCourseOffering().getSubject()
        );
        Schedule sectionB = new Schedule(
                21L,
                sectionBOffering,
                sectionA.getRoom(),
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        );
        when(roomRepository.existsById(10L)).thenReturn(true);
        when(scheduleRepository.findOfficialScheduleConflicts(10L, (short) 1, date, start, end))
                .thenReturn(List.of(sectionA, sectionB));
        when(reservationRepository.findApprovedOverlappingReservations(10L, date, start, end)).thenReturn(List.of());

        RoomAvailabilityResponse response = availabilityService.getAvailability(10L, date, start, end);

        assertThat(response.available()).isFalse();
        assertThat(response.conflicts()).hasSize(2);
        assertThat(response.conflicts()).allMatch(conflict -> conflict.type().equals("SCHEDULE"));
    }

    @Test
    @DisplayName("Approved reservation conflicts block reservation")
    void approvedReservationConflictBlocks() {
        when(scheduleRepository.findOfficialScheduleConflicts(10L, (short) 1, date, start, end)).thenReturn(List.of());
        when(reservationRepository.countApprovedOverlappingReservations(10L, date, start, end, null)).thenReturn(1L);

        assertThatThrownBy(() -> availabilityService.assertAvailableForReservation(10L, date, start, end, null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("approved reservation");
    }

    private Reservation approvedReservation() {
        Building building = new Building(1L, "Building 1", "Main");
        Floor floor = new Floor(101L, building, 1);
        Room room = new Room(10L, floor, "111", "Room 111", "CLASSROOM", "Room", "room_111");
        User user = new User(
                UUID.randomUUID(),
                "owner@example.com",
                "Secret Owner",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of()
        );
        Reservation reservation = new Reservation(user, room, date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Secret purpose");
        reservation.approve(user);
        return reservation;
    }

    private Schedule schedule() {
        Building building = new Building(1L, "Building 1", "Main");
        Floor floor = new Floor(101L, building, 1);
        Room room = new Room(10L, floor, "111", "Room 111", "CLASSROOM", "Room", "room_111");
        Semester semester = new Semester(1L, "2026 Semester 1", date.minusWeeks(1), date.plusWeeks(8));
        AcademicGroup academicGroup = new AcademicGroup(2L, (short) 1, AcademicGroupType.SECTION, "A", null, null);
        Subject subject = new Subject(3L, "CS101", "Programming", "Intro");
        CourseOffering offering = new CourseOffering(4L, semester, academicGroup, subject);
        return new Schedule(20L, offering, room, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
    }
}
