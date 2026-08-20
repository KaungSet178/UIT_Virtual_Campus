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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "semesters", schema = "public")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @OneToMany(mappedBy = "semester", fetch = FetchType.LAZY)
    @OrderBy("offeringId ASC")
    private List<CourseOffering> courseOfferings = new ArrayList<>();

    protected Semester() {
    }

    public Semester(Long semesterId, String name, LocalDate startDate, LocalDate endDate) {
        this.semesterId = semesterId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Semester(String name, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public String getName() {
        return name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<CourseOffering> getCourseOfferings() {
        return Collections.unmodifiableList(courseOfferings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Semester semester)) {
            return false;
        }
        return Objects.equals(semesterId, semester.semesterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(semesterId);
    }
}
