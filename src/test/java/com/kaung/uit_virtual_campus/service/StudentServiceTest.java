package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.dto.StudentListResponse;
import com.kaung.uit_virtual_campus.entity.AcademicGroup;
import com.kaung.uit_virtual_campus.entity.AcademicGroupType;
import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.StudentProfile;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.repository.AcademicGroupRepository;
import com.kaung.uit_virtual_campus.repository.RoleRepository;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class StudentServiceTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AcademicGroupRepository academicGroupRepository;

    @Autowired
    private EntityManager entityManager;

    private AcademicGroup sectionA;
    private AcademicGroup majorSe;

    @BeforeEach
    void setUp() {
        Role student = roleRepository.save(new Role((short) 1, "STUDENT"));
        Role admin = roleRepository.save(new Role((short) 2, "ADMIN"));
        Role clubLeader = roleRepository.save(new Role((short) 3, "CLUB_LEADER"));

        sectionA = academicGroupRepository.save(new AcademicGroup(
                (short) 1,
                AcademicGroupType.SECTION,
                "A",
                null,
                null
        ));
        majorSe = academicGroupRepository.save(new AcademicGroup(
                (short) 3,
                AcademicGroupType.MAJOR,
                null,
                "CS",
                "SE"
        ));

        User kaung = saveUser(
                "11111111-1111-1111-1111-111111111111",
                "ksthar@uit.edu.mm",
                "Kaung Set Thar",
                Set.of(student)
        );
        entityManager.persist(new StudentProfile(
                kaung,
                "UIT-TEST-001",
                null,
                null,
                (short) 1,
                sectionA
        ));

        User aye = saveUser(
                "22222222-2222-2222-2222-222222222222",
                "ayeaye@uit.edu.mm",
                "Aye Aye",
                Set.of(student, clubLeader)
        );
        entityManager.persist(new StudentProfile(
                aye,
                "UIT-TEST-002",
                "CS",
                "Software Engineering",
                (short) 3,
                majorSe
        ));

        saveUser(
                "33333333-3333-3333-3333-333333333333",
                "admin@uit.edu.mm",
                "Admin User",
                Set.of(admin)
        );

        saveUser(
                "44444444-4444-4444-4444-444444444444",
                "profile.missing@uit.edu.mm",
                "Missing Profile",
                Set.of(student)
        );

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("listStudents returns only users with STUDENT role")
    void listStudentsReturnsOnlyStudentRoleUsers() {
        List<StudentListResponse> students = studentService.listStudents(null, null, null, null);

        assertThat(students)
                .extracting(StudentListResponse::email)
                .containsExactlyInAnyOrder(
                        "ksthar@uit.edu.mm",
                        "ayeaye@uit.edu.mm",
                        "profile.missing@uit.edu.mm"
                )
                .doesNotContain("admin@uit.edu.mm");
    }

    @Test
    @DisplayName("listStudents returns student plus club leader once")
    void listStudentsReturnsStudentClubLeaderOnce() {
        List<StudentListResponse> students = studentService.listStudents(null, null, null, null);

        assertThat(students)
                .filteredOn(student -> student.email().equals("ayeaye@uit.edu.mm"))
                .hasSize(1);
    }

    @Test
    @DisplayName("listStudents searches by full name")
    void listStudentsSearchesByFullName() {
        List<StudentListResponse> students = studentService.listStudents("kaung", null, null, null);

        assertThat(students)
                .extracting(StudentListResponse::email)
                .containsExactly("ksthar@uit.edu.mm");
    }

    @Test
    @DisplayName("listStudents searches by email")
    void listStudentsSearchesByEmail() {
        List<StudentListResponse> students = studentService.listStudents("AYEAYE", null, null, null);

        assertThat(students)
                .extracting(StudentListResponse::studentNumber)
                .containsExactly("UIT-TEST-002");
    }

    @Test
    @DisplayName("listStudents searches by student number")
    void listStudentsSearchesByStudentNumber() {
        List<StudentListResponse> students = studentService.listStudents("test-001", null, null, null);

        assertThat(students)
                .extracting(StudentListResponse::fullName)
                .containsExactly("Kaung Set Thar");
    }

    @Test
    @DisplayName("listStudents filters by year")
    void listStudentsFiltersByYear() {
        List<StudentListResponse> students = studentService.listStudents(null, (short) 3, null, null);

        assertThat(students)
                .extracting(StudentListResponse::email)
                .containsExactly("ayeaye@uit.edu.mm");
    }

    @Test
    @DisplayName("listStudents filters by academic group")
    void listStudentsFiltersByAcademicGroup() {
        List<StudentListResponse> students = studentService.listStudents(null, null, sectionA.getGroupId(), null);

        assertThat(students)
                .extracting(StudentListResponse::studentNumber)
                .containsExactly("UIT-TEST-001");
    }

    @Test
    @DisplayName("listStudents maps academic group data")
    void listStudentsMapsAcademicGroupData() {
        List<StudentListResponse> students = studentService.listStudents("UIT-TEST-001", null, null, null);

        assertThat(students).hasSize(1);
        StudentListResponse student = students.get(0);
        assertThat(student.academicGroup().groupId()).isEqualTo(sectionA.getGroupId());
        assertThat(student.academicGroup().year()).isEqualTo((short) 1);
        assertThat(student.academicGroup().groupType()).isEqualTo(AcademicGroupType.SECTION);
        assertThat(student.academicGroup().sectionName()).isEqualTo("A");
        assertThat(student.academicGroup().department()).isNull();
        assertThat(student.academicGroup().major()).isNull();
    }

    @Test
    @DisplayName("listStudents maps missing student profile fields as null")
    void listStudentsMapsMissingStudentProfileAsNull() {
        List<StudentListResponse> students = studentService.listStudents("missing", null, null, null);

        assertThat(students).hasSize(1);
        StudentListResponse student = students.get(0);
        assertThat(student.email()).isEqualTo("profile.missing@uit.edu.mm");
        assertThat(student.studentNumber()).isNull();
        assertThat(student.year()).isNull();
        assertThat(student.academicGroup()).isNull();
    }

    private User saveUser(String userId, String email, String fullName, Set<Role> roles) {
        return userRepository.save(new User(
                UUID.fromString(userId),
                email,
                fullName,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                roles
        ));
    }
}
