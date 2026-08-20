package com.kaung.uit_virtual_campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "buildings", schema = "public")
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "building_name", nullable = false, unique = true)
    private String buildingName;

    @Column(name = "description")
    private String description;

    @OneToMany(mappedBy = "building", fetch = FetchType.LAZY)
    @OrderBy("floorNumber ASC")
    private List<Floor> floors = new ArrayList<>();

    protected Building() {
    }

    public Building(Long buildingId, String buildingName, String description) {
        this.buildingId = buildingId;
        this.buildingName = buildingName;
        this.description = description;
    }

    public Building(String buildingName, String description) {
        this.buildingName = buildingName;
        this.description = description;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public String getDescription() {
        return description;
    }

    public List<Floor> getFloors() {
        return Collections.unmodifiableList(floors);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Building building)) {
            return false;
        }
        return Objects.equals(buildingId, building.buildingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildingId);
    }
}
