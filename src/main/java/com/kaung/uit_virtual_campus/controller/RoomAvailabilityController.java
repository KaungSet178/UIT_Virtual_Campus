package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.RoomAvailabilityResponse;
import com.kaung.uit_virtual_campus.dto.RoomOccupancyResponse;
import com.kaung.uit_virtual_campus.service.RoomAvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/api/rooms")
public class RoomAvailabilityController {

    private final RoomAvailabilityService roomAvailabilityService;

    public RoomAvailabilityController(RoomAvailabilityService roomAvailabilityService) {
        this.roomAvailabilityService = roomAvailabilityService;
    }

    @GetMapping("/{roomId}/availability")
    public RoomAvailabilityResponse getRoomAvailability(
            @PathVariable Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime
    ) {
        return roomAvailabilityService.getAvailability(roomId, date, startTime, endTime);
    }

    // Distinct path shape from /{roomId}/availability (3 segments vs 4), so no
    // routing ambiguity. Always "right now" — this only exists to power a
    // currently-reservable/occupied filter, not an arbitrary-slot bulk check.
    @GetMapping("/availability")
    public RoomOccupancyResponse getCurrentRoomOccupancy() {
        return roomAvailabilityService.getCurrentOccupancy();
    }
}
