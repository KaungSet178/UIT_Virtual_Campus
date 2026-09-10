package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ReservationCreateRequest;
import com.kaung.uit_virtual_campus.dto.ReservationResponse;
import com.kaung.uit_virtual_campus.dto.RoomAvailabilityResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Reservation;
import com.kaung.uit_virtual_campus.entity.ReservationStatus;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.Subject;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.repository.ReservationRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
class ReservationAvailabilityConsistencyTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    private RoomAvailabilityService availabilityService;
    private ReservationService reservationService;
    private CurrentUserService currentUserService;
    private NotificationService notificationService;
    private User student;
    private Room room214;

    @BeforeEach
    void setUp() {
        currentUserService = mock(CurrentUserService.class);
        notificationService = mock(NotificationService.class);
        availabilityService =
                new RoomAvailabilityService(roomRepository, scheduleRepository, reservationRepository, Clock.systemUTC());
        reservationService = new ReservationService(
                reservationRepository,
                roomRepository,
                currentUserService,
                availabilityService,
                notificationService,
                userRepository
        );

        Building building = entityManager.persist(new Building("Building 2", "Academic"));
        Floor floor = entityManager.persist(new Floor(building, 1));
        room214 = entityManager.persist(new Room(floor, "214", "Room 214", "CLASSROOM", "Lecture room", "room_214"));
        student = entityManager.persist(new User(
                UUID.randomUUID(),
                "student@example.com",
                "Student User",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of()
        ));

        Semester semester = entityManager.persist(new Semester(
                "2026 Semester 2",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 12, 31)
        ));
        AcademicGroup sectionA = entityManager.persist(new AcademicGroup((short) 2, AcademicGroupType.SECTION, "A", null, null));
        AcademicGroup sectionB = entityManager.persist(new AcademicGroup((short) 2, AcademicGroupType.SECTION, "B", null, null));
        Subject cst2303 = entityManager.persist(new Subject("CST-2303", "Joint Lecture B/C", null));
        Subject cst2402 = entityManager.persist(new Subject("CST-2402", "Joint Lecture A/B", null));
        CourseOffering mondayOffering = entityManager.persist(new CourseOffering(semester, sectionB, cst2303));
        CourseOffering fridayOffering = entityManager.persist(new CourseOffering(semester, sectionA, cst2402));

        entityManager.persist(new Schedule(mondayOffering, room214, (short) 1, LocalTime.of(8, 30), LocalTime.of(9, 30)));
        entityManager.persist(new Schedule(fridayOffering, room214, (short) 5, LocalTime.of(8, 30), LocalTime.of(9, 30)));
        entityManager.flush();
        entityManager.clear();

        when(currentUserService.getCurrentUser()).thenReturn(student);
    }

    @Test
    @DisplayName("Room 214 Friday afternoon is available and reservation creation succeeds")
    void room214FridayAfternoonAvailabilityAndCreateAgree() {
        LocalDate friday = LocalDate.of(2026, 9, 4);
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(15, 0);

        RoomAvailabilityResponse availability = availabilityService.getAvailability(room214.getRoomId(), friday, start, end);
        ReservationResponse reservation = reservationService.createReservation(new ReservationCreateRequest(
                room214.getRoomId(),
                friday,
                start,
                end,
                "Group study"
        ));

        assertThat(friday.getDayOfWeek().getValue()).isEqualTo(5);
        assertThat(availability.available()).isTrue();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.roomNumber()).isEqualTo("214");
        assertThat(reservation.reservationDate()).isEqualTo(friday);
        assertThat(reservation.startTime()).isEqualTo(start);
        assertThat(reservation.endTime()).isEqualTo(end);
        assertThat(reservation.createdAt()).isNotNull();
        assertThat(reservation.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Room 214 Friday scheduled period is unavailable and reservation creation is rejected")
    void room214FridayScheduledPeriodAvailabilityAndCreateAgree() {
        LocalDate friday = LocalDate.of(2026, 9, 4);
        LocalTime start = LocalTime.of(8, 30);
        LocalTime end = LocalTime.of(9, 30);

        RoomAvailabilityResponse availability = availabilityService.getAvailability(room214.getRoomId(), friday, start, end);

        assertThat(availability.available()).isFalse();
        assertThat(availability.conflicts()).extracting("type").containsExactly("SCHEDULE");
        assertThatThrownBy(() -> reservationService.createReservation(new ReservationCreateRequest(
                room214.getRoomId(),
                friday,
                start,
                end,
                "Group study"
        )))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("official schedule");
    }

    @Test
    @DisplayName("Different-day scheduled time does not block Monday afternoon")
    void differentDayScheduledTimeDoesNotBlockMondayAfternoon() {
        LocalDate monday = LocalDate.of(2026, 9, 7);
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(15, 0);

        RoomAvailabilityResponse availability = availabilityService.getAvailability(room214.getRoomId(), monday, start, end);
        ReservationResponse reservation = reservationService.createReservation(new ReservationCreateRequest(
                room214.getRoomId(),
                monday,
                start,
                end,
                "Group study"
        ));

        assertThat(availability.available()).isTrue();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("Different-day overlapping wall-clock time does not block reservation")
    void differentDayOverlappingWallClockTimeDoesNotBlock() {
        LocalDate tuesday = LocalDate.of(2026, 9, 8);
        LocalTime start = LocalTime.of(8, 30);
        LocalTime end = LocalTime.of(9, 30);

        RoomAvailabilityResponse availability = availabilityService.getAvailability(room214.getRoomId(), tuesday, start, end);
        ReservationResponse reservation = reservationService.createReservation(new ReservationCreateRequest(
                room214.getRoomId(),
                tuesday,
                start,
                end,
                "Group study"
        ));

        assertThat(tuesday.getDayOfWeek().getValue()).isEqualTo(2);
        assertThat(availability.available()).isTrue();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("Same-day partial schedule overlap blocks availability and reservation creation")
    void sameDayPartialScheduleOverlapBlocksBothPaths() {
        LocalDate friday = LocalDate.of(2026, 9, 4);
        LocalTime start = LocalTime.of(9, 0);
        LocalTime end = LocalTime.of(10, 0);

        RoomAvailabilityResponse availability = availabilityService.getAvailability(room214.getRoomId(), friday, start, end);

        assertThat(availability.available()).isFalse();
        assertThat(availability.conflicts()).extracting("type").containsExactly("SCHEDULE");
        assertThatThrownBy(() -> reservationService.createReservation(new ReservationCreateRequest(
                room214.getRoomId(),
                friday,
                start,
                end,
                "Group study"
        )))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("official schedule");
    }

    @Test
    @DisplayName("Approved reservations block both availability and reservation creation")
    void approvedReservationBlocksBothPaths() {
        LocalDate friday = LocalDate.of(2026, 9, 4);
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(15, 0);
        Reservation approved = new Reservation(student, room214, friday, start, end, "Approved meeting");
        approved.approve(student);
        entityManager.persistAndFlush(approved);
        entityManager.clear();

        RoomAvailabilityResponse availability = availabilityService.getAvailability(room214.getRoomId(), friday, start, end);

        assertThat(availability.available()).isFalse();
        assertThat(availability.conflicts()).extracting("type").containsExactly("APPROVED_RESERVATION");
        assertThatThrownBy(() -> reservationService.createReservation(new ReservationCreateRequest(
                room214.getRoomId(),
                friday,
                start,
                end,
                "Group study"
        )))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("approved reservation");
    }

    @Test
    @DisplayName("Pending reservations do not block availability or another pending request")
    void pendingReservationDoesNotBlockEitherPath() {
        LocalDate friday = LocalDate.of(2026, 9, 4);
        LocalTime start = LocalTime.of(14, 0);
        LocalTime end = LocalTime.of(15, 0);
        entityManager.persistAndFlush(new Reservation(student, room214, friday, start, end, "Pending meeting"));
        entityManager.clear();

        RoomAvailabilityResponse availability = availabilityService.getAvailability(room214.getRoomId(), friday, start, end);
        ReservationResponse reservation = reservationService.createReservation(new ReservationCreateRequest(
                room214.getRoomId(),
                friday,
                start,
                end,
                "Another pending request"
        ));

        assertThat(availability.available()).isTrue();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.PENDING);
    }
}
