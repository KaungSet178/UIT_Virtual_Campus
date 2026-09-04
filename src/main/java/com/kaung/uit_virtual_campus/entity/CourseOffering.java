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
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "course_offerings",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(columnNames = {"semester_id", "academic_group_id", "subject_id"})
)
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "offering_id", nullable = false)
    private Long offeringId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_group_id", nullable = false)
    private AcademicGroup academicGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @OneToMany(mappedBy = "courseOffering", fetch = FetchType.LAZY)
    @OrderBy("dayOfWeek ASC, startTime ASC")
    private List<Schedule> schedules = new ArrayList<>();

    protected CourseOffering() {
    }

    public CourseOffering(Long offeringId, Semester semester, AcademicGroup academicGroup, Subject subject) {
        this.offeringId = offeringId;
        this.semester = semester;
        this.academicGroup = academicGroup;
        this.subject = subject;
    }

    public CourseOffering(Semester semester, AcademicGroup academicGroup, Subject subject) {
        this.semester = semester;
        this.academicGroup = academicGroup;
        this.subject = subject;
    }

    public Long getOfferingId() {
        return offeringId;
    }

    public Semester getSemester() {
        return semester;
    }

    public AcademicGroup getAcademicGroup() {
        return academicGroup;
    }

    public Subject getSubject() {
        return subject;
    }

    public List<Schedule> getSchedules() {
        return Collections.unmodifiableList(schedules);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CourseOffering that)) {
            return false;
        }
        return Objects.equals(offeringId, that.offeringId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offeringId);
    }
}
