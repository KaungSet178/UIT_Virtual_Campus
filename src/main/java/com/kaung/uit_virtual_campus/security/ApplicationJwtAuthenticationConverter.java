package com.kaung.uit_virtual_campus.security;

import com.kaung.uit_virtual_campus.entity.Role;
import com.kaung.uit_virtual_campus.entity.User;
import com.kaung.uit_virtual_campus.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class ApplicationJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(ApplicationJwtAuthenticationConverter.class);

    private final UserRepository userRepository;

    public ApplicationJwtAuthenticationConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId = parseSubject(jwt.getSubject());
        log.debug("JWT passed all validation; converting subject {} to application authorities", userId);
        Collection<GrantedAuthority> authorities = userRepository.findWithRolesByUserId(userId)
                .map(this::toAuthorities)
                .orElseGet(() -> {
                    log.warn("JWT subject passed validation but no public.users row exists for {}", userId);
                    return List.of();
                });

        return new JwtAuthenticationToken(jwt, authorities, userId.toString());
    }

    private UUID parseSubject(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            log.warn("JWT subject passed validation but is not a UUID");
            throw new BadCredentialsException("JWT subject is not a valid UUID");
        }
    }

    private List<GrantedAuthority> toAuthorities(User user) {
        return user.getRoles().stream()
                .map(Role::getRoleName)
                .map(roleName -> roleName.toUpperCase(Locale.ROOT))
                .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
