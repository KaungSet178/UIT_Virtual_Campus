package com.kaung.uit_virtual_campus.repository;

import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void resolvesUserByUuid() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        roleRepository.save(new Role((short) 1, "STUDENT"));
        userRepository.save(new User(
                userId,
                "student@uit.example",
                "Test Student",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of()
        ));

        assertThat(userRepository.findById(userId))
                .isPresent()
                .get()
                .extracting(User::getEmail)
                .isEqualTo("student@uit.example");
    }

    @Test
    void loadsUserRoles() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Role student = roleRepository.save(new Role((short) 1, "STUDENT"));
        Role clubLeader = roleRepository.save(new Role((short) 2, "CLUB_LEADER"));
        userRepository.save(new User(
                userId,
                "leader@uit.example",
                "Club Leader",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(student, clubLeader)
        ));

        assertThat(userRepository.findWithRolesByUserId(userId))
                .isPresent()
                .get()
                .extracting(user -> user.getRoles().stream().map(Role::getRoleName).toList())
                .asList()
                .containsExactlyInAnyOrder("STUDENT", "CLUB_LEADER");
    }
}
