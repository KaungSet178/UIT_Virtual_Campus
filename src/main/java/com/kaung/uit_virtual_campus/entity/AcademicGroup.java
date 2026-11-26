package com.kaung.uit_virtual_campus.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "academic_groups",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"\"year\"", "section_name"}),
                @UniqueConstraint(columnNames = {"\"year\"", "department", "major"})
        }
)
@Check(constraints = """
        "year" > 0
        AND (
            (group_type = 'SECTION' AND section_name IS NOT NULL AND department IS NULL AND major IS NULL)
            OR
            (group_type = 'DEPARTMENT' AND section_name IS NULL AND department IS NOT NULL AND major IS NULL)
            OR
            (group_type = 'MAJOR' AND section_name IS NULL AND department IS NOT NULL AND major IS NOT NULL)
        )
        """)
public class AcademicGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "\"year\"", nullable = false)
    private Short year;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false)
    private AcademicGroupType groupType;

    @Column(name = "section_name")
    private String sectionName;

    @Column(name = "department")
    private String department;

    @Column(name = "major")
    private String major;

    @OneToMany(mappedBy = "academicGroup", fetch = FetchType.LAZY)
    @OrderBy("offeringId ASC")
    private List<CourseOffering> courseOfferings = new ArrayList<>();

    protected AcademicGroup() {
    }

    public AcademicGroup(Long groupId, Short year, AcademicGroupType groupType,
                         String sectionName, String department, String major) {
        this.groupId = groupId;
        this.year = year;
        this.groupType = groupType;
        this.sectionName = sectionName;
        this.department = department;
        this.major = major;
    }

    public AcademicGroup(Short year, AcademicGroupType groupType, String sectionName, String department, String major) {
        this.year = year;
        this.groupType = groupType;
        this.sectionName = sectionName;
        this.department = department;
        this.major = major;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Short getYear() {
        return year;
    }

    public AcademicGroupType getGroupType() {
        return groupType;
    }

    public String getSectionName() {
        return sectionName;
    }

    public String getDepartment() {
        return department;
    }

    public String getMajor() {
        return major;
    }

    public List<CourseOffering> getCourseOfferings() {
        return Collections.unmodifiableList(courseOfferings);
    }

    public String getDisplayName() {
        if (groupType == AcademicGroupType.SECTION) {
            return "Year " + year + " Section " + sectionName;
        }
        if (groupType == AcademicGroupType.DEPARTMENT) {
            return "Year " + year + " " + department;
        }
        return "Year " + year + " " + department + " " + major;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AcademicGroup that)) {
            return false;
        }
        return groupId != null && Objects.equals(groupId, that.groupId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
