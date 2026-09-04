package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicGroupRepository extends JpaRepository<AcademicGroup, Long> {

    @Query("""
            SELECT ag FROM AcademicGroup ag
            WHERE (:year IS NULL OR ag.year = :year)
              AND (:groupType IS NULL OR ag.groupType = :groupType)
              AND (:department IS NULL OR ag.department = :department)
            ORDER BY ag.year ASC, ag.groupType ASC, ag.department ASC, ag.major ASC, ag.sectionName ASC
            """)
    List<AcademicGroup> findAllWithFilters(
            @Param("year") Short year,
            @Param("groupType") AcademicGroupType groupType,
            @Param("department") String department
    );
}
