package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.BuildingResponse;
import com.kaung.uit_virtual_campus.dto.FloorResponse;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.BuildingRepository;
import com.kaung.uit_virtual_campus.repository.FloorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private FloorRepository floorRepository;

    private BuildingService buildingService;

    @BeforeEach
    void setUp() {
        buildingService = new BuildingService(buildingRepository, floorRepository);
    }

    @Test
    @DisplayName("Should list all buildings in order")
    void listBuildings() {
        Building b1 = new Building(1L, "Building 1", "Desc 1");
        Building b2 = new Building(2L, "Building 2", "Desc 2");
        when(buildingRepository.findAllByOrderByBuildingIdAsc()).thenReturn(List.of(b1, b2));

        List<BuildingResponse> result = buildingService.getAllBuildings();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).buildingName()).isEqualTo("Building 1");
        assertThat(result.get(1).buildingName()).isEqualTo("Building 2");
    }

    @Test
    @DisplayName("Should get building by id when exists")
    void getBuildingByIdSuccess() {
        Building b1 = new Building(1L, "Building 1", "Desc 1");
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b1));

        BuildingResponse response = buildingService.getBuildingById(1L);

        assertThat(response.buildingId()).isEqualTo(1L);
        assertThat(response.buildingName()).isEqualTo("Building 1");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when building does not exist")
    void getBuildingByIdNotFound() {
        when(buildingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buildingService.getBuildingById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Building not found with id: 99");
    }

    @Test
    @DisplayName("Should list floors for a building")
    void listFloorsForBuilding() {
        Building b1 = new Building(1L, "Building 1", "Desc 1");
        Floor f1 = new Floor(101L, b1, 1);
        Floor f2 = new Floor(102L, b1, 2);

        when(buildingRepository.existsById(1L)).thenReturn(true);
        when(floorRepository.findByBuildingIdWithBuilding(1L)).thenReturn(List.of(f1, f2));

        List<FloorResponse> floors = buildingService.getFloorsByBuildingId(1L);

        assertThat(floors).hasSize(2);
        assertThat(floors.get(0).floorNumber()).isEqualTo(1);
        assertThat(floors.get(0).buildingName()).isEqualTo("Building 1");
        assertThat(floors.get(1).floorNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when listing floors for non-existent building")
    void listFloorsForNonExistentBuilding() {
        when(buildingRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> buildingService.getFloorsByBuildingId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Building not found with id: 99");
    }
}
