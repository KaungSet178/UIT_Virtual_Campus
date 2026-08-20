package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.BuildingResponse;
import com.kaung.uit_virtual_campus.dto.FloorResponse;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.BuildingRepository;
import com.kaung.uit_virtual_campus.repository.FloorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final FloorRepository floorRepository;

    public BuildingService(BuildingRepository buildingRepository, FloorRepository floorRepository) {
        this.buildingRepository = buildingRepository;
        this.floorRepository = floorRepository;
    }

    public List<BuildingResponse> getAllBuildings() {
        return buildingRepository.findAllByOrderByBuildingIdAsc().stream()
                .map(this::toBuildingResponse)
                .toList();
    }

    public BuildingResponse getBuildingById(Long buildingId) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new ResourceNotFoundException("Building not found with id: " + buildingId));
        return toBuildingResponse(building);
    }

    public List<FloorResponse> getFloorsByBuildingId(Long buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new ResourceNotFoundException("Building not found with id: " + buildingId);
        }
        return floorRepository.findByBuildingIdWithBuilding(buildingId).stream()
                .map(floor -> new FloorResponse(
                        floor.getFloorId(),
                        floor.getBuilding().getBuildingId(),
                        floor.getBuilding().getBuildingName(),
                        floor.getFloorNumber()
                ))
                .toList();
    }

    private BuildingResponse toBuildingResponse(Building building) {
        return new BuildingResponse(
                building.getBuildingId(),
                building.getBuildingName(),
                building.getDescription()
        );
    }
}
