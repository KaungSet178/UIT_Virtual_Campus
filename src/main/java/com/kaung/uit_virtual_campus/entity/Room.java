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
@Table(name = "rooms", schema = "public")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @Column(name = "room_name")
    private String roomName;

    @Column(name = "room_type")
    private String roomType;

    @Column(name = "description")
    private String description;

    @Column(name = "model_object_id")
    private String modelObjectId;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    @OrderBy("dayOfWeek ASC, startTime ASC")
    private List<Schedule> schedules = new ArrayList<>();

    protected Room() {
    }

    public Room(Long roomId, Floor floor, String roomNumber, String roomName,
                String roomType, String description, String modelObjectId) {
        this.roomId = roomId;
        this.floor = floor;
        this.roomNumber = roomNumber;
        this.roomName = roomName;
        this.roomType = roomType;
        this.description = description;
        this.modelObjectId = modelObjectId;
    }

    public Room(Floor floor, String roomNumber, String roomName,
                String roomType, String description, String modelObjectId) {
        this.floor = floor;
        this.roomNumber = roomNumber;
        this.roomName = roomName;
        this.roomType = roomType;
        this.description = description;
        this.modelObjectId = modelObjectId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Floor getFloor() {
        return floor;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getRoomName() {
        return roomName;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getDescription() {
        return description;
    }

    public String getModelObjectId() {
        return modelObjectId;
    }

    public List<Schedule> getSchedules() {
        return Collections.unmodifiableList(schedules);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Room room)) {
            return false;
        }
        return Objects.equals(roomId, room.roomId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId);
    }
}
