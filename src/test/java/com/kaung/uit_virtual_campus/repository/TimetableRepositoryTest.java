package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.entity.Section;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.Subject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TimetableRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    private Semester semester;
    private Section section;
    private Subject subject;
    private Room room;
    private CourseOffering offering;

    @BeforeEach
    void setUp() {
        Building building = entityManager.persist(new Building("Building 1", "Main"));
        Floor floor = entityManager.persist(new Floor(building, 1));
        room = entityManager.persist(new Room(floor, "111", "Room 111", "CLASSROOM", "Room", "room_111"));

        semester = entityManager.persist(new Semester(
                "2026 Semester 1",
                LocalDate.of(2026, 1, 5),
                LocalDate.of(2026, 5, 29)
        ));
        section = entityManager.persist(new Section("CS-1A"));
        subject = entityManager.persist(new Subject("CS101", "Programming Fundamentals", "Intro"));
        offering = entityManager.persist(new CourseOffering(semester, section, subject));
        entityManager.persist(new Schedule(offering, room, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should find course offerings with filters")
    void findCourseOfferingsWithFilters() {
        List<CourseOffering> results = courseOfferingRepository.findAllWithFilters(
                semester.getSemesterId(),
                section.getSectionId(),
                subject.getSubjectId()
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSemester().getName()).isEqualTo("2026 Semester 1");
        assertThat(results.get(0).getSection().getSectionName()).isEqualTo("CS-1A");
        assertThat(results.get(0).getSubject().getSubjectCode()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("Should find timetable entries with combined filters")
    void findTimetableWithFilters() {
        List<Schedule> results = scheduleRepository.findTimetable(
                semester.getSemesterId(),
                section.getSectionId(),
                subject.getSubjectId(),
                room.getRoomId()
        );

        assertThat(results).hasSize(1);
        Schedule schedule = results.get(0);
        assertThat(schedule.getCourseOffering().getSubject().getSubjectCode()).isEqualTo("CS101");
        assertThat(schedule.getRoom().getFloor().getBuilding().getBuildingName()).isEqualTo("Building 1");
    }

    @Test
    @DisplayName("Overlap predicate should detect partial overlaps")
    void countOverlappingSchedules() {
        long overlaps = scheduleRepository.countOverlappingSchedules(
                room.getRoomId(),
                (short) 1,
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                null
        );

        assertThat(overlaps).isEqualTo(1);
    }

    @Test
    @DisplayName("Overlap predicate should allow back-to-back schedules")
    void countBackToBackSchedulesAsNonOverlapping() {
        long overlaps = scheduleRepository.countOverlappingSchedules(
                room.getRoomId(),
                (short) 1,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                null
        );

        assertThat(overlaps).isZero();
    }
}
