package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.Building;
import com.kaung.uit_virtual_campus.entity.CourseOffering;
import com.kaung.uit_virtual_campus.entity.CourseOfferingType;
import com.kaung.uit_virtual_campus.entity.Floor;
import com.kaung.uit_virtual_campus.entity.Room;
import com.kaung.uit_virtual_campus.entity.Schedule;
import com.kaung.uit_virtual_campus.entity.Semester;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.StudentElectiveEnrollment;
import com.kaung.uit_virtual_campus.entity.Subject;
import com.kaung.uit_virtual_campus.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.hibernate.exception.ConstraintViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class TimetableRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AcademicGroupRepository academicGroupRepository;

    @Autowired
    private CourseOfferingRepository courseOfferingRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    private Semester semester;
    private AcademicGroup academicGroup;
    private Subject subject;
    private Room room;
    private CourseOffering offering;
    private Schedule schedule;

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
        academicGroup = entityManager.persist(new AcademicGroup(
                (short) 4,
                AcademicGroupType.MAJOR,
                null,
                "CS",
                "SE"
        ));
        subject = entityManager.persist(new Subject("CS101", "Programming Fundamentals", "Intro"));
        offering = entityManager.persist(new CourseOffering(semester, academicGroup, subject));
        schedule = entityManager.persist(new Schedule(offering, room, (short) 1, LocalTime.of(9, 0), LocalTime.of(10, 0)));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should find course offerings with academic group filters")
    void findCourseOfferingsWithFilters() {
        List<CourseOffering> results = courseOfferingRepository.findAllWithFilters(
                semester.getSemesterId(),
                academicGroup.getGroupId(),
                subject.getSubjectId()
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSemester().getName()).isEqualTo("2026 Semester 1");
        assertThat(results.get(0).getAcademicGroup().getMajor()).isEqualTo("SE");
        assertThat(results.get(0).getSubject().getSubjectCode()).isEqualTo("CS101");
    }

    @Test
    @DisplayName("Should find timetable entries with combined filters")
    void findTimetableWithFilters() {
        List<Schedule> results = scheduleRepository.findTimetable(
                semester.getSemesterId(),
                academicGroup.getGroupId(),
                subject.getSubjectId(),
                room.getRoomId()
        );

        assertThat(results).hasSize(1);
        Schedule schedule = results.get(0);
        assertThat(schedule.getCourseOffering().getAcademicGroup().getDepartment()).isEqualTo("CS");
        assertThat(schedule.getRoom().getFloor().getBuilding().getBuildingName()).isEqualTo("Building 1");
    }

    @Test
    @DisplayName("Overlap predicate should detect partial overlaps")
    void countOverlappingSchedules() {
        long overlaps = scheduleRepository.countDisallowedRoomOverlaps(
                room.getRoomId(),
                (short) 1,
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                semester.getSemesterId(),
                subject.getSubjectId(),
                academicGroup.getGroupId(),
                null
        );

        assertThat(overlaps).isEqualTo(1);
    }

    @Test
    @DisplayName("Overlap predicate should allow back-to-back schedules")
    void countBackToBackSchedulesAsNonOverlapping() {
        long overlaps = scheduleRepository.countDisallowedRoomOverlaps(
                room.getRoomId(),
                (short) 1,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                semester.getSemesterId(),
                subject.getSubjectId(),
                academicGroup.getGroupId(),
                null
        );

        assertThat(overlaps).isZero();
    }

    @Test
    @DisplayName("Exact same slot with same subject and semester for another academic group is allowed")
    void exactSameSlotSameSubjectAndSemesterDifferentGroupAllowed() {
        AcademicGroup otherGroup = entityManager.persist(new AcademicGroup(
                (short) 4,
                AcademicGroupType.MAJOR,
                null,
                "CS",
                "KE"
        ));
        entityManager.flush();

        long overlaps = scheduleRepository.countDisallowedRoomOverlaps(
                room.getRoomId(),
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                semester.getSemesterId(),
                subject.getSubjectId(),
                otherGroup.getGroupId(),
                null
        );

        assertThat(overlaps).isZero();
    }

    @Test
    @DisplayName("Exact same slot with different subject is rejected")
    void exactSameSlotDifferentSubjectRejected() {
        AcademicGroup otherGroup = entityManager.persist(new AcademicGroup(
                (short) 4,
                AcademicGroupType.MAJOR,
                null,
                "CS",
                "KE"
        ));
        Subject otherSubject = entityManager.persist(new Subject("CS102", "Data Structures", "Intro"));
        entityManager.flush();

        long overlaps = scheduleRepository.countDisallowedRoomOverlaps(
                room.getRoomId(),
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                semester.getSemesterId(),
                otherSubject.getSubjectId(),
                otherGroup.getGroupId(),
                null
        );

        assertThat(overlaps).isEqualTo(1);
    }

    @Test
    @DisplayName("Overlapping different time boundaries with same subject is rejected")
    void partialOverlapSameSubjectRejected() {
        AcademicGroup otherGroup = entityManager.persist(new AcademicGroup(
                (short) 4,
                AcademicGroupType.MAJOR,
                null,
                "CS",
                "KE"
        ));
        entityManager.flush();

        long overlaps = scheduleRepository.countDisallowedRoomOverlaps(
                room.getRoomId(),
                (short) 1,
                LocalTime.of(9, 30),
                LocalTime.of(10, 30),
                semester.getSemesterId(),
                subject.getSubjectId(),
                otherGroup.getGroupId(),
                null
        );

        assertThat(overlaps).isEqualTo(1);
    }

    @Test
    @DisplayName("Exact same slot for the same academic group remains rejected")
    void sameAcademicGroupDuplicateRejected() {
        long overlaps = scheduleRepository.countDisallowedRoomOverlaps(
                room.getRoomId(),
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                semester.getSemesterId(),
                subject.getSubjectId(),
                academicGroup.getGroupId(),
                null
        );

        assertThat(overlaps).isEqualTo(1);
    }

    @Test
    @DisplayName("Update conflict check excludes the schedule being updated")
    void updateConflictCheckExcludesCurrentSchedule() {
        long overlaps = scheduleRepository.countDisallowedRoomOverlaps(
                room.getRoomId(),
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                semester.getSemesterId(),
                subject.getSubjectId(),
                academicGroup.getGroupId(),
                schedule.getScheduleId()
        );

        assertThat(overlaps).isZero();
    }

    @Test
    @DisplayName("Academic group maps SECTION shape")
    void academicGroupMapsSectionShape() {
        AcademicGroup sectionGroup = entityManager.persist(new AcademicGroup(
                (short) 1,
                AcademicGroupType.SECTION,
                "A",
                null,
                null
        ));
        entityManager.flush();
        entityManager.clear();

        AcademicGroup loaded = entityManager.find(AcademicGroup.class, sectionGroup.getGroupId());

        assertThat(loaded.getGroupType()).isEqualTo(AcademicGroupType.SECTION);
        assertThat(loaded.getSectionName()).isEqualTo("A");
        assertThat(loaded.getDepartment()).isNull();
        assertThat(loaded.getMajor()).isNull();
    }

    @Test
    @DisplayName("Academic group maps Year 3 CS DEPARTMENT shape")
    void academicGroupMapsYearThreeCsDepartmentShape() {
        AcademicGroup departmentGroup = entityManager.persist(new AcademicGroup(
                (short) 3,
                AcademicGroupType.DEPARTMENT,
                null,
                "CS",
                null
        ));
        entityManager.flush();
        entityManager.clear();

        AcademicGroup loaded = entityManager.find(AcademicGroup.class, departmentGroup.getGroupId());

        assertThat(loaded.getGroupType()).isEqualTo(AcademicGroupType.DEPARTMENT);
        assertThat(loaded.getYear()).isEqualTo((short) 3);
        assertThat(loaded.getSectionName()).isNull();
        assertThat(loaded.getDepartment()).isEqualTo("CS");
        assertThat(loaded.getMajor()).isNull();
    }

    @Test
    @DisplayName("Academic group maps Year 3 CT DEPARTMENT shape")
    void academicGroupMapsYearThreeCtDepartmentShape() {
        AcademicGroup departmentGroup = entityManager.persist(new AcademicGroup(
                (short) 3,
                AcademicGroupType.DEPARTMENT,
                null,
                "CT",
                null
        ));
        entityManager.flush();
        entityManager.clear();

        AcademicGroup loaded = entityManager.find(AcademicGroup.class, departmentGroup.getGroupId());

        assertThat(loaded.getGroupType()).isEqualTo(AcademicGroupType.DEPARTMENT);
        assertThat(loaded.getYear()).isEqualTo((short) 3);
        assertThat(loaded.getSectionName()).isNull();
        assertThat(loaded.getDepartment()).isEqualTo("CT");
        assertThat(loaded.getMajor()).isNull();
    }

    @Test
    @DisplayName("Academic group rejects invalid year")
    void academicGroupRejectsInvalidYear() {
        AcademicGroup invalid = new AcademicGroup((short) 0, AcademicGroupType.SECTION, "A", null, null);

        assertThatThrownBy(() -> {
            entityManager.persist(invalid);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Academic group rejects invalid type shape")
    void academicGroupRejectsInvalidTypeShape() {
        AcademicGroup invalid = new AcademicGroup((short) 1, AcademicGroupType.SECTION, "A", "CS", null);

        assertThatThrownBy(() -> {
            entityManager.persist(invalid);
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Academic group enforces section uniqueness by year and section name")
    void academicGroupEnforcesSectionUniqueness() {
        entityManager.persist(new AcademicGroup((short) 1, AcademicGroupType.SECTION, "A", null, null));

        assertThatThrownBy(() -> {
            entityManager.persist(new AcademicGroup((short) 1, AcademicGroupType.SECTION, "A", null, null));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Academic group enforces major uniqueness by year department and major")
    void academicGroupEnforcesMajorUniqueness() {
        assertThatThrownBy(() -> {
            entityManager.persist(new AcademicGroup((short) 4, AcademicGroupType.MAJOR, null, "CS", "SE"));
            entityManager.flush();
        }).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("Student profile loads assigned academic group")
    void studentProfileLoadsAssignedAcademicGroup() {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        User user = entityManager.persist(new User(
                userId,
                "student@uit.example",
                "Student User",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of()
        ));
        entityManager.persist(new StudentProfile(user, "UIT-003", "CS", "Software Engineering", (short) 3, academicGroup));
        entityManager.flush();
        entityManager.clear();

        Optional<StudentProfile> loaded = studentProfileRepository.findWithUserByStudentId(userId);

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getProgramme()).isEqualTo("Software Engineering");
        assertThat(loaded.get().getAcademicGroup().getGroupId()).isEqualTo(academicGroup.getGroupId());
        assertThat(loaded.get().getAcademicGroup().getMajor()).isEqualTo("SE");
    }

    @Test
    @DisplayName("Student profile allows null academic group")
    void studentProfileAllowsNullAcademicGroup() {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        User user = entityManager.persist(new User(
                userId,
                "unassigned@uit.example",
                "Unassigned Student",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of()
        ));
        entityManager.persist(new StudentProfile(user, "UIT-004", "CS", "Software Engineering", (short) 3, null));
        entityManager.flush();
        entityManager.clear();

        Optional<StudentProfile> loaded = studentProfileRepository.findWithUserByStudentId(userId);

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getAcademicGroup()).isNull();
    }

    @Test
    @DisplayName("Personal timetable query filters by academic group and active semester")
    void personalTimetableFiltersByAcademicGroupAndSemester() {
        AcademicGroup otherGroup = entityManager.persist(new AcademicGroup((short) 4, AcademicGroupType.MAJOR, null, "CS", "KE"));
        Semester otherSemester = entityManager.persist(new Semester(
                "2026 Semester 2",
                LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 10, 30)
        ));
        CourseOffering otherGroupOffering = entityManager.persist(new CourseOffering(semester, otherGroup, subject));
        CourseOffering otherSemesterOffering = entityManager.persist(new CourseOffering(otherSemester, academicGroup, subject));
        entityManager.persist(new Schedule(otherGroupOffering, room, (short) 2, LocalTime.of(8, 0), LocalTime.of(9, 0)));
        entityManager.persist(new Schedule(otherSemesterOffering, room, (short) 3, LocalTime.of(8, 0), LocalTime.of(9, 0)));
        entityManager.persist(new Schedule(offering, room, (short) 1, LocalTime.of(11, 0), LocalTime.of(12, 0)));
        entityManager.flush();
        entityManager.clear();

        List<Semester> active = semesterRepository
                .findByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAscSemesterIdAsc(
                        LocalDate.of(2026, 2, 1),
                        LocalDate.of(2026, 2, 1)
                );
        List<Schedule> results = scheduleRepository.findPersonalTimetable(
                semester.getSemesterId(),
                academicGroup.getGroupId()
        );

        assertThat(active).extracting(Semester::getSemesterId).containsExactly(semester.getSemesterId());
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Schedule::getDayOfWeek).containsExactly((short) 1, (short) 1);
        assertThat(results).extracting(Schedule::getStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(11, 0));
    }

    @Test
    @DisplayName("Personal timetable query returns mandatory schedules only")
    void personalTimetableReturnsMandatorySchedulesOnly() {
        Subject electiveSubject = entityManager.persist(new Subject("CST-4105", "Java Keystone", null));
        CourseOffering electiveOffering = entityManager.persist(new CourseOffering(
                semester,
                academicGroup,
                electiveSubject,
                CourseOfferingType.ELECTIVE
        ));
        entityManager.persist(new Schedule(electiveOffering, room, (short) 2, LocalTime.of(13, 0), LocalTime.of(14, 0)));
        entityManager.flush();
        entityManager.clear();

        List<Schedule> results = scheduleRepository.findPersonalTimetable(
                semester.getSemesterId(),
                academicGroup.getGroupId()
        );

        assertThat(results).extracting(schedule -> schedule.getCourseOffering().getType())
                .containsOnly(CourseOfferingType.MANDATORY);
        assertThat(results).extracting(schedule -> schedule.getCourseOffering().getSubject().getSubjectCode())
                .doesNotContain("CST-4105");
    }

    @Test
    @DisplayName("Student elective timetable query returns only selected elective schedules")
    void studentElectiveTimetableReturnsSelectedElectivesOnly() {
        UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        User user = entityManager.persist(new User(
                userId,
                "student-elective@uit.example",
                "Elective Student",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of()
        ));
        StudentProfile profile = entityManager.persist(new StudentProfile(
                user,
                "UIT-005",
                "CS",
                "Software Engineering",
                (short) 3,
                academicGroup
        ));
        Subject selectedSubject = entityManager.persist(new Subject("CST-4105", "Java Keystone", null));
        Subject notSelectedSubject = entityManager.persist(new Subject("CST-4408", "Cybersecurity", null));
        CourseOffering selectedOffering = entityManager.persist(new CourseOffering(
                semester,
                academicGroup,
                selectedSubject,
                CourseOfferingType.ELECTIVE
        ));
        CourseOffering notSelectedOffering = entityManager.persist(new CourseOffering(
                semester,
                academicGroup,
                notSelectedSubject,
                CourseOfferingType.ELECTIVE
        ));
        entityManager.persist(new Schedule(selectedOffering, room, (short) 2, LocalTime.of(13, 0), LocalTime.of(14, 0)));
        entityManager.persist(new Schedule(notSelectedOffering, room, (short) 3, LocalTime.of(13, 0), LocalTime.of(14, 0)));
        entityManager.persist(new StudentElectiveEnrollment(profile, selectedOffering));
        entityManager.flush();
        entityManager.clear();

        List<Schedule> results = scheduleRepository.findStudentElectiveSchedules(userId, semester.getSemesterId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCourseOffering().getSubject().getSubjectCode()).isEqualTo("CST-4105");
        assertThat(results.get(0).getCourseOffering().getType()).isEqualTo(CourseOfferingType.ELECTIVE);
    }
}
