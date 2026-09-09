package com.kaung.uit_virtual_campus.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record RoomOccupancyResponse(
        LocalDate date,
        LocalTime time,
        List<Long> occupiedRoomIds
) {
}
