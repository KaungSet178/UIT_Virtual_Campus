package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.RoomType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CampusRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private FloorRepository floorRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Building building1;
    private Building building2;
    private Floor floor1_1;
    private Floor floor2_3;
    private Room room111;
    private Room room231;
    private Room roomMph;

    @BeforeEach
    void setUp() {
        building1 = entityManager.persist(new Building("Building 1", "Main Academic Building"));
        building2 = entityManager.persist(new Building("Building 2", "Lab Building"));

        floor1_1 = entityManager.persist(new Floor(building1, 1));
        floor2_3 = entityManager.persist(new Floor(building2, 3));

        room111 = entityManager.persist(new Room(floor1_1, "111", "Room 111", RoomType.CLASSROOM, "Classroom 111", "room_111"));
        room231 = entityManager.persist(new Room(floor2_3, "231", "Database Lab", RoomType.LABORATORY, "DB Lab", "room_231"));
        roomMph = entityManager.persist(new Room(floor1_1, "MPH", "Multipurpose Hall", RoomType.MULTIPURPOSE_HALL, "Campus Hall", "room_mph"));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("BuildingRepository finds all buildings ordered by ID")
    void findAllBuildings() {
        List<Building> buildings = buildingRepository.findAllByOrderByBuildingIdAsc();
        assertThat(buildings).hasSize(2);
        assertThat(buildings.get(0).getBuildingName()).isEqualTo("Building 1");
        assertThat(buildings.get(1).getBuildingName()).isEqualTo("Building 2");
    }

    @Test
    @DisplayName("FloorRepository finds floors by building ID with eagerly loaded building")
    void findFloorsByBuilding() {
        List<Floor> floors = floorRepository.findByBuildingIdWithBuilding(building1.getBuildingId());
        assertThat(floors).hasSize(1);
        assertThat(floors.get(0).getFloorNumber()).isEqualTo(1);
        assertThat(floors.get(0).getBuilding().getBuildingName()).isEqualTo("Building 1");
    }

    @Test
    @DisplayName("RoomRepository finds room by ID with full hierarchy")
    void findRoomByIdWithHierarchy() {
        Optional<Room> found = roomRepository.findByIdWithHierarchy(room231.getRoomId());
        assertThat(found).isPresent();
        Room r = found.get();
        assertThat(r.getRoomNumber()).isEqualTo("231");
        assertThat(r.getRoomName()).isEqualTo("Database Lab");
        assertThat(r.getFloor().getFloorNumber()).isEqualTo(3);
        assertThat(r.getFloor().getBuilding().getBuildingName()).isEqualTo("Building 2");
    }

    @Test
    @DisplayName("RoomRepository searches rooms by room number case-insensitively")
    void searchByRoomNumber() {
        List<Room> results = roomRepository.searchRooms("231");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRoomNumber()).isEqualTo("231");
    }

    @Test
    @DisplayName("RoomRepository searches rooms by room name case-insensitively")
    void searchByRoomName() {
        List<Room> results = roomRepository.searchRooms("database");
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getRoomName()).isEqualTo("Database Lab");

        List<Room> hallResults = roomRepository.searchRooms("multipurpose");
        assertThat(hallResults).hasSize(1);
        assertThat(hallResults.get(0).getRoomName()).isEqualTo("Multipurpose Hall");
    }
}
