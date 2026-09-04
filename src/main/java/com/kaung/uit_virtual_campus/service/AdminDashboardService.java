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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final ScheduleRepository scheduleRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            RoomRepository roomRepository,
            ReservationRepository reservationRepository,
            ClubRepository clubRepository,
            EventRepository eventRepository,
            ScheduleRepository scheduleRepository
    ) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.clubRepository = clubRepository;
        this.eventRepository = eventRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public AdminDashboardSummaryResponse getSummary() {
        return new AdminDashboardSummaryResponse(
                userRepository.count(),
                studentProfileRepository.count(),
                roomRepository.count(),
                reservationRepository.countByStatus(ReservationStatus.PENDING),
                clubRepository.count(),
                eventRepository.countByEndDatetimeGreaterThanEqual(OffsetDateTime.now()),
                scheduleRepository.count()
        );
    }
}
