package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.RoomResponse;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
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
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    private CampusValidationService validationService;
    private RoomService roomService;

    private Building building;
    private Floor floor;
    private Room room;

    @BeforeEach
    void setUp() {
        validationService = new CampusValidationService();
        roomService = new RoomService(roomRepository, validationService);

        building = new Building(2L, "Building 2", "Academic Building 2");
        floor = new Floor(203L, building, 3);
        room = new Room(10L, floor, "231", "Database Lab", "LABORATORY", "DB Lab desc", "room_231");
    }

    @Test
    @DisplayName("Should list all rooms")
    void listRooms() {
        when(roomRepository.findAllWithHierarchy()).thenReturn(List.of(room));

        List<RoomResponse> rooms = roomService.getAllRooms();

        assertThat(rooms).hasSize(1);
        RoomResponse resp = rooms.get(0);
        assertThat(resp.roomId()).isEqualTo(10L);
        assertThat(resp.roomNumber()).isEqualTo("231");
        assertThat(resp.roomName()).isEqualTo("Database Lab");
        assertThat(resp.roomType()).isEqualTo("LABORATORY");
        assertThat(resp.buildingId()).isEqualTo(2L);
        assertThat(resp.buildingName()).isEqualTo("Building 2");
        assertThat(resp.floorId()).isEqualTo(203L);
        assertThat(resp.floorNumber()).isEqualTo(3);
        assertThat(resp.modelObjectId()).isEqualTo("room_231");
    }

    @Test
    @DisplayName("Should get room by id when exists")
    void getRoomByIdSuccess() {
        when(roomRepository.findByIdWithHierarchy(10L)).thenReturn(Optional.of(room));

        RoomResponse resp = roomService.getRoomById(10L);

        assertThat(resp.roomId()).isEqualTo(10L);
        assertThat(resp.roomNumber()).isEqualTo("231");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when room does not exist")
    void getRoomByIdNotFound() {
        when(roomRepository.findByIdWithHierarchy(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room not found with id: 999");
    }

    @Test
    @DisplayName("Should search rooms by room number or name")
    void searchRooms() {
        when(roomRepository.searchRooms("231")).thenReturn(List.of(room));

        List<RoomResponse> results = roomService.searchRooms("231");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).roomNumber()).isEqualTo("231");
    }

    @Test
    @DisplayName("Should return empty list for empty/null search query")
    void searchRoomsEmptyQuery() {
        assertThat(roomService.searchRooms(null)).isEmpty();
        assertThat(roomService.searchRooms("   ")).isEmpty();
    }
}
