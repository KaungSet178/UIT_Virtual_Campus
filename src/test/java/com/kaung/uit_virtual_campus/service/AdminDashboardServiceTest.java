package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.AdminDashboardSummaryResponse;
import com.kaung.uit_virtual_campus.entity.ReservationStatus;
import com.kaung.uit_virtual_campus.repository.ClubRepository;
import com.kaung.uit_virtual_campus.repository.EventRepository;
import com.kaung.uit_virtual_campus.repository.ReservationRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import com.kaung.uit_virtual_campus.repository.StudentProfileRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("Admin dashboard summary returns repository counts")
    void adminDashboardSummaryReturnsCounts() {
        when(userRepository.count()).thenReturn(2L);
        when(studentProfileRepository.count()).thenReturn(1L);
        when(roomRepository.count()).thenReturn(144L);
        when(reservationRepository.countByStatus(ReservationStatus.PENDING)).thenReturn(3L);
        when(clubRepository.count()).thenReturn(4L);
        when(eventRepository.countByEndDatetimeGreaterThanEqual(any(OffsetDateTime.class))).thenReturn(5L);
        when(scheduleRepository.count()).thenReturn(112L);
        AdminDashboardService service = new AdminDashboardService(
                userRepository,
                studentProfileRepository,
                roomRepository,
                reservationRepository,
                clubRepository,
                eventRepository,
                scheduleRepository
        );

        AdminDashboardSummaryResponse response = service.getSummary();

        assertThat(response.totalUsers()).isEqualTo(2L);
        assertThat(response.totalStudents()).isEqualTo(1L);
        assertThat(response.totalRooms()).isEqualTo(144L);
        assertThat(response.pendingReservations()).isEqualTo(3L);
        assertThat(response.totalClubs()).isEqualTo(4L);
        assertThat(response.upcomingEvents()).isEqualTo(5L);
        assertThat(response.totalSchedules()).isEqualTo(112L);
        verify(reservationRepository).countByStatus(ReservationStatus.PENDING);
        verify(eventRepository).countByEndDatetimeGreaterThanEqual(any(OffsetDateTime.class));
    }
}
