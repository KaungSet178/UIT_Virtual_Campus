package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.RoomAvailabilityResponse;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Reservation;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.entity.Section;
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
        availabilityService = new RoomAvailabilityService(roomRepository, scheduleRepository, reservationRepository);
        date = LocalDate.of(2026, 8, 24);
        start = LocalTime.of(9, 30);
        end = LocalTime.of(10, 30);
    }

    @Test
    @DisplayName("Availability response redacts approved reservation owner and purpose")
    void availabilityRedactsReservationDetails() {
        Reservation reservation = approvedReservation();
        when(roomRepository.existsById(10L)).thenReturn(true);
        when(scheduleRepository.findOfficialScheduleConflicts(10L, (short) 1, date, start, end)).thenReturn(List.of());
        when(reservationRepository.findApprovedOverlappingReservations(10L, date, start, end)).thenReturn(List.of(reservation));

        RoomAvailabilityResponse response = availabilityService.getAvailability(10L, date, start, end);

        assertThat(response.available()).isFalse();
        assertThat(response.conflicts()).hasSize(1);
        assertThat(response.conflicts().get(0).type()).isEqualTo("APPROVED_RESERVATION");
        assertThat(response.conflicts().get(0).description()).isEqualTo("Approved reservation");
        assertThat(response.conflicts().get(0).description()).doesNotContain("Secret", "owner@example.com");
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
        assertThat(response.conflicts().get(0).description()).contains("CS101", "CS-1A");
    }

    @Test
    @DisplayName("Approved reservation conflicts block reservation")
    void approvedReservationConflictBlocks() {
        when(scheduleRepository.countOfficialScheduleConflicts(10L, (short) 1, date, start, end)).thenReturn(0L);
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
        Section section = new Section(2L, "CS-1A");
        Subject subject = new Subject(3L, "CS101", "Programming", "Intro");
        CourseOffering offering = new CourseOffering(4L, semester, section, subject);
        return new Schedule(20L, offering, room, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
    }
}
