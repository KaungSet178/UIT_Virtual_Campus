package com.kaung.uit_virtual_campus.service;

import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.exception.AuthenticatedUserNotFoundException;
import com.kaung.uit_virtual_campus.exception.InvalidJwtSubjectException;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UUID getCurrentUserId() {
        return UUID.fromString(getCurrentJwt().getSubject());
    }

    public Optional<String> getCurrentUserEmail() {
        return Optional.ofNullable(getCurrentJwt().getClaimAsString("email"))
                .map(String::trim)
                .filter(email -> !email.isEmpty());
    }

    private Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new InvalidJwtSubjectException("");
        }

        try {
            UUID.fromString(jwt.getSubject());
            return jwt;
        } catch (IllegalArgumentException exception) {
            throw new InvalidJwtSubjectException(jwt.getSubject());
        }
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        UUID userId = getCurrentUserId();
        return userRepository.findWithRolesByUserId(userId)
                .orElseThrow(() -> new AuthenticatedUserNotFoundException(userId));
    }
}
