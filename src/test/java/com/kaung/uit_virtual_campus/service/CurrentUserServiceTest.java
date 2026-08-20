package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CurrentUserService currentUserService = new CurrentUserService(userRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesAuthenticatedUserFromJwtSubject() {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        User user = new User(
                userId,
                "admin@uit.example",
                "Admin User",
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                Set.of(new Role((short) 3, "ADMIN"))
        );
        when(userRepository.findWithRolesByUserId(userId)).thenReturn(Optional.of(user));
        Jwt jwt = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", userId.toString())
        );
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        User resolvedUser = currentUserService.getCurrentUser();

        assertThat(resolvedUser.getUserId()).isEqualTo(userId);
        assertThat(resolvedUser.getRoles())
                .extracting(Role::getRoleName)
                .containsExactly("ADMIN");
    }
}
