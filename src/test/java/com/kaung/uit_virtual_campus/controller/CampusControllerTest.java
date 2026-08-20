package com.kaung.uit_virtual_campus.controller;

import com.kaung.uit_virtual_campus.dto.BuildingResponse;
import com.kaung.uit_virtual_campus.dto.FloorResponse;
import com.kaung.uit_virtual_campus.dto.RoomResponse;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.service.BuildingService;
import com.kaung.uit_virtual_campus.service.FloorService;
import com.kaung.uit_virtual_campus.service.RoomService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CampusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BuildingService buildingService;

    @MockitoBean
    private FloorService floorService;

    @MockitoBean
    private RoomService roomService;

    // --- Unauthenticated Security Tests (401) ---

    @Test
    @DisplayName("Unauthenticated request to /api/buildings returns 401")
    void unauthenticatedBuildingsReturns401() throws Exception {
        mockMvc.perform(get("/api/buildings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/floors/1 returns 401")
    void unauthenticatedFloorsReturns401() throws Exception {
        mockMvc.perform(get("/api/floors/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated request to /api/rooms returns 401")
    void unauthenticatedRoomsReturns401() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized());
    }

    // --- BuildingController Tests ---

    @Test
    @WithMockUser
    @DisplayName("GET /api/buildings returns list of buildings")
    void listBuildings() throws Exception {
        BuildingResponse b1 = new BuildingResponse(1L, "Building 1", "Main Academic Building");
        BuildingResponse b2 = new BuildingResponse(2L, "Building 2", "Lab Building");
        when(buildingService.getAllBuildings()).thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/buildings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].buildingId").value(1))
                .andExpect(jsonPath("$[0].buildingName").value("Building 1"))
                .andExpect(jsonPath("$[1].buildingId").value(2));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/buildings/{buildingId} returns building when found")
    void getBuildingFound() throws Exception {
        BuildingResponse b1 = new BuildingResponse(1L, "Building 1", "Main Academic Building");
        when(buildingService.getBuildingById(1L)).thenReturn(b1);

        mockMvc.perform(get("/api/buildings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buildingId").value(1))
                .andExpect(jsonPath("$.buildingName").value("Building 1"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/buildings/{buildingId} returns 404 when not found")
    void getBuildingNotFound() throws Exception {
        when(buildingService.getBuildingById(99L))
                .thenThrow(new ResourceNotFoundException("Building not found with id: 99"));

        mockMvc.perform(get("/api/buildings/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Building not found with id: 99"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/buildings/{buildingId}/floors returns floors")
    void listFloorsForBuilding() throws Exception {
        FloorResponse f1 = new FloorResponse(101L, 1L, "Building 1", 1);
        FloorResponse f2 = new FloorResponse(102L, 1L, "Building 1", 2);
        when(buildingService.getFloorsByBuildingId(1L)).thenReturn(List.of(f1, f2));

        mockMvc.perform(get("/api/buildings/1/floors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].floorId").value(101))
                .andExpect(jsonPath("$[0].floorNumber").value(1))
                .andExpect(jsonPath("$[1].floorNumber").value(2));
    }

    // --- FloorController Tests ---

    @Test
    @WithMockUser
    @DisplayName("GET /api/floors/{floorId} returns floor when found")
    void getFloorFound() throws Exception {
        FloorResponse f1 = new FloorResponse(101L, 1L, "Building 1", 1);
        when(floorService.getFloorById(101L)).thenReturn(f1);

        mockMvc.perform(get("/api/floors/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.floorId").value(101))
                .andExpect(jsonPath("$.buildingName").value("Building 1"))
                .andExpect(jsonPath("$.floorNumber").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/floors/{floorId} returns 404 when not found")
    void getFloorNotFound() throws Exception {
        when(floorService.getFloorById(999L))
                .thenThrow(new ResourceNotFoundException("Floor not found with id: 999"));

        mockMvc.perform(get("/api/floors/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/floors/{floorId}/rooms returns rooms on that floor")
    void listRoomsForFloor() throws Exception {
        RoomResponse r1 = new RoomResponse(10L, "231", "Database Lab", "LABORATORY", "Lab desc", 2L, "Building 2", 203L, 3, "room_231");
        when(floorService.getRoomsByFloorId(203L)).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/floors/203/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roomId").value(10))
                .andExpect(jsonPath("$[0].roomNumber").value("231"))
                .andExpect(jsonPath("$[0].modelObjectId").value("room_231"));
    }

    // --- RoomController Tests ---

    @Test
    @WithMockUser
    @DisplayName("GET /api/rooms returns list of all rooms")
    void listRooms() throws Exception {
        RoomResponse r1 = new RoomResponse(10L, "231", "Database Lab", "LABORATORY", "Lab desc", 2L, "Building 2", 203L, 3, "room_231");
        when(roomService.getAllRooms()).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roomNumber").value("231"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/rooms/{roomId} returns room when found")
    void getRoomFound() throws Exception {
        RoomResponse r1 = new RoomResponse(10L, "231", "Database Lab", "LABORATORY", "Lab desc", 2L, "Building 2", 203L, 3, "room_231");
        when(roomService.getRoomById(10L)).thenReturn(r1);

        mockMvc.perform(get("/api/rooms/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(10))
                .andExpect(jsonPath("$.roomNumber").value("231"))
                .andExpect(jsonPath("$.roomName").value("Database Lab"))
                .andExpect(jsonPath("$.buildingName").value("Building 2"))
                .andExpect(jsonPath("$.floorNumber").value(3))
                .andExpect(jsonPath("$.modelObjectId").value("room_231"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/rooms/{roomId} returns 404 when not found")
    void getRoomNotFound() throws Exception {
        when(roomService.getRoomById(999L))
                .thenThrow(new ResourceNotFoundException("Room not found with id: 999"));

        mockMvc.perform(get("/api/rooms/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/rooms/search with query returns matching rooms")
    void searchRooms() throws Exception {
        RoomResponse r1 = new RoomResponse(10L, "231", "Database Lab", "LABORATORY", "Lab desc", 2L, "Building 2", 203L, 3, "room_231");
        when(roomService.searchRooms("231")).thenReturn(List.of(r1));

        mockMvc.perform(get("/api/rooms/search").param("query", "231"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].roomNumber").value("231"))
                .andExpect(jsonPath("$[0].roomName").value("Database Lab"));
    }
}
