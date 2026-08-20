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
@Table(name = "sections", schema = "public")
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "section_name", nullable = false, unique = true)
    private String sectionName;

    @OneToMany(mappedBy = "section", fetch = FetchType.LAZY)
    @OrderBy("offeringId ASC")
    private List<CourseOffering> courseOfferings = new ArrayList<>();

    protected Section() {
    }

    public Section(Long sectionId, String sectionName) {
        this.sectionId = sectionId;
        this.sectionName = sectionName;
    }

    public Section(String sectionName) {
        this.sectionName = sectionName;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public List<CourseOffering> getCourseOfferings() {
        return Collections.unmodifiableList(courseOfferings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Section section)) {
            return false;
        }
        return Objects.equals(sectionId, section.sectionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sectionId);
    }
}
