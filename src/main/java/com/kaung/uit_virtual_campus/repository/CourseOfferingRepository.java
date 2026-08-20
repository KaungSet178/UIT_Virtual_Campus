package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    boolean existsBySemesterSemesterIdAndSectionSectionIdAndSubjectSubjectId(
            Long semesterId,
            Long sectionId,
            Long subjectId
    );

    @Query("""
            SELECT co FROM CourseOffering co
            JOIN FETCH co.semester sem
            JOIN FETCH co.section sec
            JOIN FETCH co.subject sub
            WHERE co.offeringId = :offeringId
            """)
    Optional<CourseOffering> findByIdWithDetails(@Param("offeringId") Long offeringId);

    @Query("""
            SELECT co FROM CourseOffering co
            JOIN FETCH co.semester sem
            JOIN FETCH co.section sec
            JOIN FETCH co.subject sub
            WHERE (:semesterId IS NULL OR sem.semesterId = :semesterId)
              AND (:sectionId IS NULL OR sec.sectionId = :sectionId)
              AND (:subjectId IS NULL OR sub.subjectId = :subjectId)
            ORDER BY sem.startDate DESC, sec.sectionName ASC, sub.subjectCode ASC
            """)
    List<CourseOffering> findAllWithFilters(
            @Param("semesterId") Long semesterId,
            @Param("sectionId") Long sectionId,
            @Param("subjectId") Long subjectId
    );
}
