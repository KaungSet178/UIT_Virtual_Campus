package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.entity.RoomType;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class CampusValidationService {

    private static final Pattern THREE_DIGIT_PATTERN = Pattern.compile("^[0-9]{3}$");

    /**
     * Checks if a room number is a 3-digit string.
     */
    public boolean isNumberedRoomFormat(String roomNumber) {
        return roomNumber != null && THREE_DIGIT_PATTERN.matcher(roomNumber.trim()).matches();
    }

    /**
     * Validates ABC format for numbered rooms:
     * A = Building (1-4)
     * B = Floor (1-6)
     * C = Room (1-6)
     */
    public boolean isValidNumberedRoom(String roomNumber) {
        if (roomNumber == null) {
            return false;
        }
        String trimmed = roomNumber.trim();
        if (trimmed.length() != 3) {
            return false;
        }

        char buildingChar = trimmed.charAt(0);
        char floorChar = trimmed.charAt(1);
        char roomChar = trimmed.charAt(2);

        boolean validBuilding = buildingChar >= '1' && buildingChar <= '4';
        boolean validFloor = floorChar >= '1' && floorChar <= '6';
        boolean validRoom = roomChar >= '1' && roomChar <= '6';

        return validBuilding && validFloor && validRoom;
    }

    /**
     * Checks if a room number is exempt from ABC numbering (e.g. Multipurpose Hall).
     */
    public boolean isExemptFromNumbering(String roomNumber, String roomType) {
        if (RoomType.MULTIPURPOSE_HALL.equalsIgnoreCase(roomType)) {
            return true;
        }
        return roomNumber != null && !THREE_DIGIT_PATTERN.matcher(roomNumber.trim()).matches();
    }

    /**
     * Validates whether a numbered room's identifier matches its parent building and floor in the hierarchy.
     */
    public boolean isHierarchyConsistent(String roomNumber, Long buildingId, Integer floorNumber) {
        if (roomNumber == null || !isValidNumberedRoom(roomNumber)) {
            return false;
        }
        String trimmed = roomNumber.trim();
        int expectedBuildingId = Character.getNumericValue(trimmed.charAt(0));
        int expectedFloorNumber = Character.getNumericValue(trimmed.charAt(1));

        if (buildingId != null && buildingId != expectedBuildingId) {
            return false;
        }
        if (floorNumber != null && floorNumber != expectedFloorNumber) {
            return false;
        }
        return true;
    }

    /**
     * Performs domain validation on a room.
     */
    public void validateRoom(String roomNumber, String roomType, Long buildingId, Integer floorNumber) {
        if (isExemptFromNumbering(roomNumber, roomType)) {
            return;
        }

        if (!isValidNumberedRoom(roomNumber)) {
            throw new IllegalArgumentException(
                    "Invalid numbered room: " + roomNumber + ". Must follow ABC format (A=Building 1-4, B=Floor 1-6, C=Room 1-6)."
            );
        }

        if (!isHierarchyConsistent(roomNumber, buildingId, floorNumber)) {
            throw new IllegalArgumentException(
                    "Room number " + roomNumber + " does not match hierarchy building " + buildingId + " and floor " + floorNumber + "."
            );
        }
    }
}
