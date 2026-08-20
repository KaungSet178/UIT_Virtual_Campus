package com.kaung.uit_virtual_campus.dto;

public record RoomResponse(
        Long roomId,
        String roomNumber,
        String roomName,
        String roomType,
        String description,
        Long buildingId,
        String buildingName,
        Long floorId,
        Integer floorNumber,
        String modelObjectId
) {
}
