package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.StudentProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    @EntityGraph(attributePaths = "user")
    Optional<StudentProfile> findWithUserByStudentId(UUID studentId);
}
