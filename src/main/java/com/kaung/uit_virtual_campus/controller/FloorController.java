package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.FloorResponse;
import com.kaung.uit_virtual_campus.dto.RoomResponse;
import com.kaung.uit_virtual_campus.service.FloorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/floors")
public class FloorController {

    private final FloorService floorService;

    public FloorController(FloorService floorService) {
        this.floorService = floorService;
    }

    @GetMapping("/{floorId}")
    public FloorResponse getFloor(@PathVariable Long floorId) {
        return floorService.getFloorById(floorId);
    }

    @GetMapping("/{floorId}/rooms")
    public List<RoomResponse> listRoomsForFloor(@PathVariable Long floorId) {
        return floorService.getRoomsByFloorId(floorId);
    }
}
