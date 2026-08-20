package com.kaung.uit_virtual_campus.dto;

import java.time.LocalDate;

public record SemesterResponse(
        Long semesterId,
        String name,
        LocalDate startDate,
        LocalDate endDate
) {
}
