package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.RoomResponse;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;
    private final CampusValidationService campusValidationService;

    public RoomService(RoomRepository roomRepository, CampusValidationService campusValidationService) {
        this.roomRepository = roomRepository;
        this.campusValidationService = campusValidationService;
    }

    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAllWithHierarchy().stream()
                .map(this::toRoomResponse)
                .toList();
    }

    public RoomResponse getRoomById(Long roomId) {
        Room room = roomRepository.findByIdWithHierarchy(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));
        return toRoomResponse(room);
    }

    public List<RoomResponse> searchRooms(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return roomRepository.searchRooms(query.trim()).stream()
                .map(this::toRoomResponse)
                .toList();
    }

    public boolean isValidNumberedRoom(String roomNumber) {
        return campusValidationService.isValidNumberedRoom(roomNumber);
    }

    public void validateRoom(String roomNumber, String roomType, Long buildingId, Integer floorNumber) {
        campusValidationService.validateRoom(roomNumber, roomType, buildingId, floorNumber);
    }

    private RoomResponse toRoomResponse(Room room) {
        return new RoomResponse(
                room.getRoomId(),
                room.getRoomNumber(),
                room.getRoomName(),
                room.getRoomType(),
                room.getDescription(),
                room.getFloor().getBuilding().getBuildingId(),
                room.getFloor().getBuilding().getBuildingName(),
                room.getFloor().getFloorId(),
                room.getFloor().getFloorNumber(),
                room.getModelObjectId()
        );
    }
}
