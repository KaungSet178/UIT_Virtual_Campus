package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.FloorResponse;
import com.kaung.uit_virtual_campus.dto.RoomResponse;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.FloorRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
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
class FloorServiceTest {

    @Mock
    private FloorRepository floorRepository;

    @Mock
    private RoomRepository roomRepository;

    private FloorService floorService;

    @BeforeEach
    void setUp() {
        floorService = new FloorService(floorRepository, roomRepository);
    }

    @Test
    @DisplayName("Should get floor by id when exists")
    void getFloorByIdSuccess() {
        Building b = new Building(2L, "Building 2", "Desc");
        Floor f = new Floor(203L, b, 3);
        when(floorRepository.findByIdWithBuilding(203L)).thenReturn(Optional.of(f));

        FloorResponse response = floorService.getFloorById(203L);

        assertThat(response.floorId()).isEqualTo(203L);
        assertThat(response.buildingId()).isEqualTo(2L);
        assertThat(response.buildingName()).isEqualTo("Building 2");
        assertThat(response.floorNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when floor does not exist")
    void getFloorByIdNotFound() {
        when(floorRepository.findByIdWithBuilding(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> floorService.getFloorById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Floor not found with id: 999");
    }

    @Test
    @DisplayName("Should list rooms for a floor")
    void listRoomsForFloor() {
        Building b = new Building(2L, "Building 2", "Desc");
        Floor f = new Floor(203L, b, 3);
        Room r1 = new Room(1L, f, "231", "Database Lab", "LABORATORY", "Lab desc", "room_231");
        Room r2 = new Room(2L, f, "232", "Room 232", "CLASSROOM", "Classroom desc", "room_232");

        when(floorRepository.existsById(203L)).thenReturn(true);
        when(roomRepository.findByFloorIdWithHierarchy(203L)).thenReturn(List.of(r1, r2));

        List<RoomResponse> rooms = floorService.getRoomsByFloorId(203L);

        assertThat(rooms).hasSize(2);
        assertThat(rooms.get(0).roomNumber()).isEqualTo("231");
        assertThat(rooms.get(0).roomName()).isEqualTo("Database Lab");
        assertThat(rooms.get(0).modelObjectId()).isEqualTo("room_231");
        assertThat(rooms.get(1).roomNumber()).isEqualTo("232");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when listing rooms for non-existent floor")
    void listRoomsForNonExistentFloor() {
        when(floorRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> floorService.getRoomsByFloorId(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Floor not found with id: 999");
    }
}
