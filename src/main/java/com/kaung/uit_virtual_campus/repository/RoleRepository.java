package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Short> {

    Optional<Role> findByRoleNameIgnoreCase(String roleName);
}
