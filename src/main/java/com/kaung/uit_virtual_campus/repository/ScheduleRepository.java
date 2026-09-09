package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.courseOffering co
            JOIN FETCH co.semester sem
            JOIN FETCH co.academicGroup ag
            JOIN FETCH co.subject sub
            JOIN FETCH s.room r
            JOIN FETCH r.floor f
            JOIN FETCH f.building b
            WHERE s.scheduleId = :scheduleId
            """)
    Optional<Schedule> findByIdWithDetails(@Param("scheduleId") Long scheduleId);

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.courseOffering co
            JOIN FETCH co.semester sem
            JOIN FETCH co.academicGroup ag
            JOIN FETCH co.subject sub
            JOIN FETCH s.room r
            JOIN FETCH r.floor f
            JOIN FETCH f.building b
            WHERE (:semesterId IS NULL OR sem.semesterId = :semesterId)
              AND (:academicGroupId IS NULL OR ag.groupId = :academicGroupId)
              AND (:subjectId IS NULL OR sub.subjectId = :subjectId)
              AND (:roomId IS NULL OR r.roomId = :roomId)
            ORDER BY sem.startDate DESC, ag.year ASC, ag.groupType ASC, ag.department ASC, ag.major ASC, ag.sectionName ASC, s.dayOfWeek ASC, s.startTime ASC, sub.subjectCode ASC
            """)
    List<Schedule> findTimetable(
            @Param("semesterId") Long semesterId,
            @Param("academicGroupId") Long academicGroupId,
            @Param("subjectId") Long subjectId,
            @Param("roomId") Long roomId
    );

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.courseOffering co
            JOIN FETCH co.semester sem
            JOIN FETCH co.academicGroup ag
            JOIN FETCH co.subject sub
            JOIN FETCH s.room r
            JOIN FETCH r.floor f
            JOIN FETCH f.building b
            WHERE sem.semesterId = :semesterId
              AND ag.groupId = :academicGroupId
              AND co.type = 'MANDATORY'
            ORDER BY s.dayOfWeek ASC, s.startTime ASC, sub.subjectCode ASC
            """)
    List<Schedule> findPersonalTimetable(
            @Param("semesterId") Long semesterId,
            @Param("academicGroupId") Long academicGroupId
    );

    @Query("""
            SELECT DISTINCT s FROM Schedule s
            JOIN FETCH s.courseOffering co
            JOIN FETCH co.semester sem
            JOIN FETCH co.academicGroup ag
            JOIN FETCH co.subject sub
            JOIN FETCH s.room r
            JOIN FETCH r.floor f
            JOIN FETCH f.building b
            JOIN StudentElectiveEnrollment enrollment ON enrollment.courseOffering = co
            WHERE enrollment.studentProfile.studentId = :studentProfileId
              AND sem.semesterId = :semesterId
              AND co.type = 'ELECTIVE'
            ORDER BY s.dayOfWeek ASC, s.startTime ASC, s.endTime ASC, sub.subjectCode ASC
            """)
    List<Schedule> findStudentElectiveSchedules(
            @Param("studentProfileId") UUID studentProfileId,
            @Param("semesterId") Long semesterId
    );

    @Query("""
            SELECT COUNT(s) FROM Schedule s
            JOIN s.courseOffering co
            JOIN co.semester sem
            JOIN co.academicGroup ag
            JOIN co.subject sub
            WHERE s.room.roomId = :roomId
              AND s.dayOfWeek = :dayOfWeek
              AND s.startTime < :requestedEnd
              AND s.endTime > :requestedStart
              AND (:excludedScheduleId IS NULL OR s.scheduleId <> :excludedScheduleId)
              AND NOT (
                    sem.semesterId = :semesterId
                AND sub.subjectId = :subjectId
                AND ag.groupId <> :academicGroupId
                AND s.startTime = :requestedStart
                AND s.endTime = :requestedEnd
              )
            """)
    long countDisallowedRoomOverlaps(
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") Short dayOfWeek,
            @Param("requestedStart") LocalTime requestedStart,
            @Param("requestedEnd") LocalTime requestedEnd,
            @Param("semesterId") Long semesterId,
            @Param("subjectId") Long subjectId,
            @Param("academicGroupId") Long academicGroupId,
            @Param("excludedScheduleId") Long excludedScheduleId
    );

    @Query("""
            SELECT COUNT(existing) FROM Schedule existing
            JOIN existing.courseOffering existingOffering
            WHERE existingOffering.semester.semesterId = :semesterId
              AND existingOffering.academicGroup.groupId = :academicGroupId
              AND existingOffering.type = 'MANDATORY'
              AND existing.dayOfWeek = :dayOfWeek
              AND existing.startTime < :requestedEnd
              AND existing.endTime > :requestedStart
            """)
    long countMandatoryTimetableOverlaps(
            @Param("semesterId") Long semesterId,
            @Param("academicGroupId") Long academicGroupId,
            @Param("dayOfWeek") Short dayOfWeek,
            @Param("requestedStart") LocalTime requestedStart,
            @Param("requestedEnd") LocalTime requestedEnd
    );

    @Query("""
            SELECT COUNT(existing) FROM Schedule existing
            JOIN existing.courseOffering existingOffering
            JOIN StudentElectiveEnrollment enrollment ON enrollment.courseOffering = existingOffering
            WHERE enrollment.studentProfile.studentId = :studentProfileId
              AND existingOffering.semester.semesterId = :semesterId
              AND existingOffering.type = 'ELECTIVE'
              AND existingOffering.offeringId <> :requestedOfferingId
              AND existing.dayOfWeek = :dayOfWeek
              AND existing.startTime < :requestedEnd
              AND existing.endTime > :requestedStart
            """)
    long countStudentElectiveOverlaps(
            @Param("studentProfileId") UUID studentProfileId,
            @Param("semesterId") Long semesterId,
            @Param("requestedOfferingId") Long requestedOfferingId,
            @Param("dayOfWeek") Short dayOfWeek,
            @Param("requestedStart") LocalTime requestedStart,
            @Param("requestedEnd") LocalTime requestedEnd
    );

    @Query("""
            SELECT s FROM Schedule s
            JOIN FETCH s.courseOffering co
            JOIN FETCH co.semester sem
            JOIN FETCH co.academicGroup ag
            JOIN FETCH co.subject sub
            JOIN FETCH s.room r
            JOIN FETCH r.floor f
            JOIN FETCH f.building b
            WHERE r.roomId = :roomId
              AND s.dayOfWeek = :dayOfWeek
              AND sem.startDate <= :requestedDate
              AND sem.endDate >= :requestedDate
              AND s.startTime < :requestedEnd
              AND s.endTime > :requestedStart
            ORDER BY s.startTime ASC
            """)
    List<Schedule> findOfficialScheduleConflicts(
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") Short dayOfWeek,
            @Param("requestedDate") LocalDate requestedDate,
            @Param("requestedStart") LocalTime requestedStart,
            @Param("requestedEnd") LocalTime requestedEnd
    );

    // Same predicate as findOfficialScheduleConflicts minus the roomId filter,
    // for the bulk "which rooms are occupied right now" check — selecting the
    // distinct room id directly is cheaper than fetching whole Schedule rows
    // for something that only needs a yes/no per room.
    @Query("""
            SELECT DISTINCT s.room.roomId FROM Schedule s
            JOIN s.courseOffering co
            JOIN co.semester sem
            WHERE s.dayOfWeek = :dayOfWeek
              AND sem.startDate <= :requestedDate
              AND sem.endDate >= :requestedDate
              AND s.startTime < :requestedEnd
              AND s.endTime > :requestedStart
            """)
    List<Long> findOccupiedRoomIdsBySchedule(
            @Param("dayOfWeek") Short dayOfWeek,
            @Param("requestedDate") LocalDate requestedDate,
            @Param("requestedStart") LocalTime requestedStart,
            @Param("requestedEnd") LocalTime requestedEnd
    );
}
