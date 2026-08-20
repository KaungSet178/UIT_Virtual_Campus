package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("SELECT r FROM Room r JOIN FETCH r.floor f JOIN FETCH f.building b WHERE r.roomId = :roomId")
    Optional<Room> findByIdWithHierarchy(@Param("roomId") Long roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.roomId = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);

    @Query("SELECT r FROM Room r JOIN FETCH r.floor f JOIN FETCH f.building b WHERE f.floorId = :floorId ORDER BY r.roomNumber ASC")
    List<Room> findByFloorIdWithHierarchy(@Param("floorId") Long floorId);

    @Query("SELECT r FROM Room r JOIN FETCH r.floor f JOIN FETCH f.building b ORDER BY b.buildingId ASC, f.floorNumber ASC, r.roomNumber ASC")
    List<Room> findAllWithHierarchy();

    @Query("SELECT r FROM Room r JOIN FETCH r.floor f JOIN FETCH f.building b WHERE LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(r.roomName) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY b.buildingId ASC, f.floorNumber ASC, r.roomNumber ASC")
    List<Room> searchRooms(@Param("query") String query);
}
