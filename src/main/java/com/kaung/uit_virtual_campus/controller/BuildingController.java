package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.BuildingResponse;
import com.kaung.uit_virtual_campus.dto.FloorResponse;
import com.kaung.uit_virtual_campus.service.BuildingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping
    public List<BuildingResponse> listBuildings() {
        return buildingService.getAllBuildings();
    }

    @GetMapping("/{buildingId}")
    public BuildingResponse getBuilding(@PathVariable Long buildingId) {
        return buildingService.getBuildingById(buildingId);
    }

    @GetMapping("/{buildingId}/floors")
    public List<FloorResponse> listFloorsForBuilding(@PathVariable Long buildingId) {
        return buildingService.getFloorsByBuildingId(buildingId);
    }
}
