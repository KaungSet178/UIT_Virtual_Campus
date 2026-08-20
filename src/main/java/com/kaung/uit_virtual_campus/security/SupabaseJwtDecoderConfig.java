package com.kaung.uit_virtual_campus.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Collection;

@Configuration
public class SupabaseJwtDecoderConfig {

    private static final Logger log = LoggerFactory.getLogger(SupabaseJwtDecoderConfig.class);

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${app.security.jwt.audience}") String audience
    ) {
        String jwkSetUri = issuerUri + "/.well-known/jwks.json";
        log.debug("Building JWT decoder: issuer={}, audience={}, jwkSetUri={}", issuerUri, audience, jwkSetUri);

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri)
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();

        OAuth2TokenValidator<Jwt> issuerAndTimestampValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> hasAudience(jwt, audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token",
                "JWT audience does not contain the required audience",
                null
        ));

        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                issuerAndTimestampValidator,
                audienceValidator
        ));

        return jwt -> {
            try {
                Jwt decoded = jwtDecoder.decode(jwt);
                log.debug("JWT decode succeeded for subject {}", decoded.getSubject());
                return decoded;
            } catch (JwtException exception) {
                log.warn("JWT decode rejected a token: {}: {}", exception.getClass().getSimpleName(),
                        exception.getMessage());
                throw exception;
            }
        };
    }

    private boolean hasAudience(Jwt jwt, String requiredAudience) {
        Object audienceClaim = jwt.getClaims().get("aud");
        if (audienceClaim instanceof String audience) {
            return requiredAudience.equals(audience);
        }
        if (audienceClaim instanceof Collection<?> audiences) {
            return audiences.contains(requiredAudience);
        }
        return false;
    }
}
