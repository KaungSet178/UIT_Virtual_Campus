package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.StudentElectiveEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface StudentElectiveEnrollmentRepository extends JpaRepository<StudentElectiveEnrollment, Long> {

    boolean existsByStudentProfileStudentIdAndCourseOfferingOfferingId(UUID studentProfileId, Long courseOfferingId);

    Optional<StudentElectiveEnrollment> findByStudentProfileStudentIdAndCourseOfferingOfferingId(
            UUID studentProfileId,
            Long courseOfferingId
    );

    @Query("""
            SELECT DISTINCT enrollment FROM StudentElectiveEnrollment enrollment
            JOIN FETCH enrollment.courseOffering co
            JOIN FETCH co.semester sem
            JOIN FETCH co.academicGroup ag
            JOIN FETCH co.subject sub
            LEFT JOIN FETCH co.schedules schedules
            LEFT JOIN FETCH schedules.room room
            LEFT JOIN FETCH room.floor floor
            LEFT JOIN FETCH floor.building building
            WHERE enrollment.studentProfile.studentId = :studentProfileId
              AND sem.semesterId = :semesterId
            ORDER BY sub.subjectCode ASC, enrollment.id ASC
            """)
    List<StudentElectiveEnrollment> findCurrentEnrollments(
            @Param("studentProfileId") UUID studentProfileId,
            @Param("semesterId") Long semesterId
    );

    @Query("""
            SELECT enrollment.courseOffering.offeringId FROM StudentElectiveEnrollment enrollment
            WHERE enrollment.studentProfile.studentId = :studentProfileId
              AND enrollment.courseOffering.semester.semesterId = :semesterId
            """)
    Set<Long> findCurrentEnrolledOfferingIds(
            @Param("studentProfileId") UUID studentProfileId,
            @Param("semesterId") Long semesterId
    );
}
