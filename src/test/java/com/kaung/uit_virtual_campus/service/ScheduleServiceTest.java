package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.ScheduleRequest;
import com.kaung.uit_virtual_campus.dto.ScheduleResponse;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.entity.Section;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.Subject;
import com.kaung.uit_virtual_campus.repository.CourseOfferingRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private CourseOfferingRepository courseOfferingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private TimetableValidationService validationService;

    private ScheduleService scheduleService;
    private CourseOffering offering;
    private Room room;
    private Schedule schedule;

    @BeforeEach
    void setUp() {
        scheduleService = new ScheduleService(
                scheduleRepository,
                courseOfferingRepository,
                roomRepository,
                validationService
        );

        Semester semester = new Semester(1L, "2026 Semester 1", LocalDate.of(2026, 1, 5), LocalDate.of(2026, 5, 29));
        Section section = new Section(2L, "CS-1A");
        Subject subject = new Subject(3L, "CS101", "Programming Fundamentals", "Intro");
        offering = new CourseOffering(4L, semester, section, subject);
        Building building = new Building(1L, "Building 1", "Main");
        Floor floor = new Floor(101L, building, 1);
        room = new Room(10L, floor, "111", "Room 111", "CLASSROOM", "Room", "room_111");
        schedule = new Schedule(20L, offering, room, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("Should create schedule after validation")
    void createSchedule() {
        ScheduleRequest request = new ScheduleRequest(4L, 10L, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
        when(courseOfferingRepository.findByIdWithDetails(4L)).thenReturn(Optional.of(offering));
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));
        when(scheduleRepository.save(org.mockito.ArgumentMatchers.any(Schedule.class))).thenReturn(schedule);
        when(scheduleRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(schedule));

        ScheduleResponse response = scheduleService.createSchedule(request);

        assertThat(response.scheduleId()).isEqualTo(20L);
        assertThat(response.dayName()).isEqualTo("MONDAY");
        verify(validationService).validateScheduleTime((short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
        verify(validationService).validateNoRoomOverlap(10L, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0), null);
        verify(roomRepository).findByIdForUpdate(10L);
    }

    @Test
    @DisplayName("Should exclude current schedule during update overlap validation")
    void updateScheduleExcludesCurrentSchedule() {
        ScheduleRequest request = new ScheduleRequest(4L, 10L, (short) 2, LocalTime.of(10, 0), LocalTime.of(11, 0));
        Schedule updated = new Schedule(20L, offering, room, (short) 2, LocalTime.of(10, 0), LocalTime.of(11, 0));
        when(scheduleRepository.findById(20L)).thenReturn(Optional.of(schedule));
        when(courseOfferingRepository.findByIdWithDetails(4L)).thenReturn(Optional.of(offering));
        when(roomRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(room));
        when(scheduleRepository.findByIdWithDetails(20L)).thenReturn(Optional.of(updated));

        ScheduleResponse response = scheduleService.updateSchedule(20L, request);

        assertThat(response.dayOfWeek()).isEqualTo((short) 2);
        verify(validationService).validateNoRoomOverlap(10L, (short) 2, LocalTime.of(10, 0), LocalTime.of(11, 0), 20L);
        verify(roomRepository).findByIdForUpdate(10L);
    }
}
