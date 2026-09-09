package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesByUserId(UUID userId);

    @Query("""
            SELECT DISTINCT u.userId FROM User u
            JOIN u.roles r
            WHERE UPPER(r.roleName) = UPPER(:roleName)
            """)
    List<UUID> findUserIdsByRoleName(@Param("roleName") String roleName);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN FETCH u.roles roles
            LEFT JOIN FETCH u.studentProfile sp
            LEFT JOIN FETCH sp.academicGroup ag
            WHERE UPPER(roles.roleName) = 'STUDENT'
            AND (
                :query IS NULL
                OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(sp.studentNumber) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            AND (:year IS NULL OR sp.year = :year)
            AND (:academicGroupId IS NULL OR ag.groupId = :academicGroupId)
            AND (:department IS NULL OR LOWER(sp.department) = LOWER(:department))
            ORDER BY u.fullName ASC, u.userId ASC
            """)
    List<User> findStudentsForList(
            @Param("query") String query,
            @Param("year") Short year,
            @Param("academicGroupId") Long academicGroupId,
            @Param("department") String department
    );
}
