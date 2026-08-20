package com.kaung.uit_virtual_campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "floors", schema = "public")
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "floor_id", nullable = false)
    private Long floorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    @OneToMany(mappedBy = "floor", fetch = FetchType.LAZY)
    @OrderBy("roomNumber ASC")
    private List<Room> rooms = new ArrayList<>();

    protected Floor() {
    }

    public Floor(Long floorId, Building building, Integer floorNumber) {
        this.floorId = floorId;
        this.building = building;
        this.floorNumber = floorNumber;
    }

    public Floor(Building building, Integer floorNumber) {
        this.building = building;
        this.floorNumber = floorNumber;
    }

    public Long getFloorId() {
        return floorId;
    }

    public Building getBuilding() {
        return building;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public List<Room> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Floor floor)) {
            return false;
        }
        return Objects.equals(floorId, floor.floorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(floorId);
    }
}
