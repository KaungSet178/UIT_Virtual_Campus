package com.kaung.uit_virtual_campus.dto;

public record FloorResponse(
        Long floorId,
        Long buildingId,
        String buildingName,
        Integer floorNumber
) {
}
