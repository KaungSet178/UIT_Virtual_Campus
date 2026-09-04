package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
            ORDER BY s.dayOfWeek ASC, s.startTime ASC, sub.subjectCode ASC
            """)
    List<Schedule> findPersonalTimetable(
            @Param("semesterId") Long semesterId,
            @Param("academicGroupId") Long academicGroupId
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
}
