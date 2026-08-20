package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

    List<Building> findAllByOrderByBuildingIdAsc();
}
