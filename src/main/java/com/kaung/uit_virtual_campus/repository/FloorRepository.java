package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FloorRepository extends JpaRepository<Floor, Long> {

    @Query("SELECT f FROM Floor f JOIN FETCH f.building b WHERE f.building.buildingId = :buildingId ORDER BY f.floorNumber ASC")
    List<Floor> findByBuildingIdWithBuilding(@Param("buildingId") Long buildingId);

    @Query("SELECT f FROM Floor f JOIN FETCH f.building b WHERE f.floorId = :floorId")
    Optional<Floor> findByIdWithBuilding(@Param("floorId") Long floorId);
}
