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
}
