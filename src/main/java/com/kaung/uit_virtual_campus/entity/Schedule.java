package com.kaung.uit_virtual_campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "schedules", schema = "public")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering courseOffering;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /**
     * ISO-8601 day number: 1=Monday through 7=Sunday.
     */
    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    protected Schedule() {
    }

    public Schedule(Long scheduleId, CourseOffering courseOffering, Room room,
                    Short dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.scheduleId = scheduleId;
        this.courseOffering = courseOffering;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Schedule(CourseOffering courseOffering, Room room,
                    Short dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.courseOffering = courseOffering;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public CourseOffering getCourseOffering() {
        return courseOffering;
    }

    public Room getRoom() {
        return room;
    }

    public Short getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void update(CourseOffering courseOffering, Room room, Short dayOfWeek,
                       LocalTime startTime, LocalTime endTime) {
        this.courseOffering = courseOffering;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Schedule schedule)) {
            return false;
        }
        return Objects.equals(scheduleId, schedule.scheduleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduleId);
    }
}
