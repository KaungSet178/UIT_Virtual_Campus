package com.kaung.uit_virtual_campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "staff_profiles", schema = "public")
public class StaffProfile {

    @Id
    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "staff_id", nullable = false)
    private User user;

    @Column(name = "employee_number")
    private String employeeNumber;

    @Column(name = "department")
    private String department;

    @Column(name = "position")
    private String position;

    protected StaffProfile() {
    }

    public StaffProfile(User user, String employeeNumber, String department, String position) {
        this.user = user;
        this.staffId = user.getUserId();
        this.employeeNumber = employeeNumber;
        this.department = department;
        this.position = position;
    }

    public UUID getStaffId() {
        return staffId;
    }

    public User getUser() {
        return user;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public String getDepartment() {
        return department;
    }

    public String getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StaffProfile that)) {
            return false;
        }
        return staffId != null && Objects.equals(staffId, that.staffId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
