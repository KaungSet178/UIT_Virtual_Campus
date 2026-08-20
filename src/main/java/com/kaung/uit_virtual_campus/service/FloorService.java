package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.FloorResponse;
import com.kaung.uit_virtual_campus.dto.RoomResponse;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.exception.ResourceNotFoundException;
import com.kaung.uit_virtual_campus.repository.FloorRepository;
import com.kaung.uit_virtual_campus.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FloorService {

    private final FloorRepository floorRepository;
    private final RoomRepository roomRepository;

    public FloorService(FloorRepository floorRepository, RoomRepository roomRepository) {
        this.floorRepository = floorRepository;
        this.roomRepository = roomRepository;
    }

    public FloorResponse getFloorById(Long floorId) {
        Floor floor = floorRepository.findByIdWithBuilding(floorId)
                .orElseThrow(() -> new ResourceNotFoundException("Floor not found with id: " + floorId));
        return toFloorResponse(floor);
    }

    public List<RoomResponse> getRoomsByFloorId(Long floorId) {
        if (!floorRepository.existsById(floorId)) {
            throw new ResourceNotFoundException("Floor not found with id: " + floorId);
        }
        return roomRepository.findByFloorIdWithHierarchy(floorId).stream()
                .map(this::toRoomResponse)
                .toList();
    }

    private FloorResponse toFloorResponse(Floor floor) {
        return new FloorResponse(
                floor.getFloorId(),
                floor.getBuilding().getBuildingId(),
                floor.getBuilding().getBuildingName(),
                floor.getFloorNumber()
        );
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
