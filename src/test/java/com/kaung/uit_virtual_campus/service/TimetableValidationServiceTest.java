package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.exception.ConflictException;
import com.kaung.uit_virtual_campus.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimetableValidationServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    private TimetableValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new TimetableValidationService(scheduleRepository);
    }

    @Test
    @DisplayName("Should reject day outside ISO 1-7 range")
    void rejectsInvalidDay() {
        assertThatThrownBy(() -> validationService.validateScheduleTime(
                (short) 8,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dayOfWeek must be between 1 and 7");
    }

    @Test
    @DisplayName("Should reject invalid time range")
    void rejectsInvalidTimeRange() {
        assertThatThrownBy(() -> validationService.validateScheduleTime(
                (short) 1,
                LocalTime.of(10, 0),
                LocalTime.of(10, 0)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    @DisplayName("Should reject overlapping room schedule")
    void rejectsRoomOverlap() {
        when(scheduleRepository.countDisallowedRoomOverlaps(
                10L,
                (short) 1,
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                1L,
                3L,
                2L,
                null
        )).thenReturn(1L);

        assertThatThrownBy(() -> validationService.validateNoRoomOverlap(
                10L,
                (short) 1,
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                1L,
                3L,
                2L,
                null
        ))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlaps");
    }

    @Test
    @DisplayName("Should allow non-overlapping schedule")
    void allowsNoRoomOverlap() {
        when(scheduleRepository.countDisallowedRoomOverlaps(
                10L,
                (short) 1,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                1L,
                3L,
                2L,
                null
        )).thenReturn(0L);

        assertThatCode(() -> validationService.validateNoRoomOverlap(
                10L,
                (short) 1,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                1L,
                3L,
                2L,
                null
        )).doesNotThrowAnyException();
    }
}
