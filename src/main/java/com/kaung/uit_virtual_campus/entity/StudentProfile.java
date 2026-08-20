package com.kaung.uit_virtual_campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "student_profiles", schema = "public")
public class StudentProfile {

    @Id
    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "student_id", nullable = false)
    private User user;

    @Column(name = "student_number", nullable = false, unique = true)
    private String studentNumber;

    @Column(name = "department")
    private String department;

    @Column(name = "\"year\"")
    private Short year;

    @OneToMany(mappedBy = "student", fetch = FetchType.LAZY)
    @OrderBy("joinedAt DESC")
    private List<ClubMembership> memberships = new ArrayList<>();

    protected StudentProfile() {
    }

    public StudentProfile(User user, String studentNumber, String department, Short year) {
        this.user = user;
        this.studentId = user.getUserId();
        this.studentNumber = studentNumber;
        this.department = department;
        this.year = year;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public User getUser() {
        return user;
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public String getDepartment() {
        return department;
    }

    public Short getYear() {
        return year;
    }

    public List<ClubMembership> getMemberships() {
        return Collections.unmodifiableList(memberships);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StudentProfile that)) {
            return false;
        }
        return studentId != null && Objects.equals(studentId, that.studentId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
