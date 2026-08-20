package com.kaung.uit_virtual_campus.security;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SupabaseJwtDecoderConfigTest {

    private static final String ISSUER_BASE = "http://127.0.0.1:";
    private static final String AUDIENCE = "authenticated";

    private static final ECKey SIGNING_KEY;
    private static final int JWKS_PORT;

    static {
        try {
            SIGNING_KEY = new ECKeyGenerator(Curve.P_256)
                    .keyID("uit-test-key")
                    .algorithm(JWSAlgorithm.ES256)
                    .generate();
            String jwksBody = "{\"keys\":[" + SIGNING_KEY.toPublicJWK().toJSONString() + "]}";
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/auth/v1/.well-known/jwks.json", exchange -> {
                byte[] body = jwksBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            JWKS_PORT = server.getAddress().getPort();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> ISSUER_BASE + JWKS_PORT + "/auth/v1");
        registry.add("app.security.jwt.audience", () -> AUDIENCE);
    }

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void validEs256SupabaseStyleTokenDecodes() throws Exception {
        Jwt jwt = jwtDecoder.decode(signEs256(ISSUER_BASE + JWKS_PORT + "/auth/v1", AUDIENCE,
                "44444444-4444-4444-4444-444444444444"));

        assertThat(jwt.getSubject()).isEqualTo("44444444-4444-4444-4444-444444444444");
        assertThat(jwt.getAudience()).contains(AUDIENCE);
        assertThat(jwt.getIssuer().toString()).isEqualTo(ISSUER_BASE + JWKS_PORT + "/auth/v1");
    }

    @Test
    void tokenWithWrongAudienceIsRejected() throws Exception {
        String token = signEs256(ISSUER_BASE + JWKS_PORT + "/auth/v1", "service_role",
                "44444444-4444-4444-4444-444444444444");
        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithWrongIssuerIsRejected() throws Exception {
        String token = signEs256("https://evil.example.com/auth/v1", AUDIENCE,
                "44444444-4444-4444-4444-444444444444");
        assertThatThrownBy(() -> jwtDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithLegacyHs256IsRejected() throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(ISSUER_BASE + JWKS_PORT + "/auth/v1")
                .audience(AUDIENCE)
                .subject("44444444-4444-4444-4444-444444444444")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                claimsSet);
        signedJWT.sign(new MACSigner("this-is-a-fake-supabase-jwt-secret-32-bytes!!"));
        assertThatThrownBy(() -> jwtDecoder.decode(signedJWT.serialize())).isInstanceOf(JwtException.class);
    }

    private String signEs256(String issuer, String audience, String subject) throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject(subject)
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .build();
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).type(JOSEObjectType.JWT)
                        .keyID(SIGNING_KEY.getKeyID())
                        .build(),
                claimsSet);
        signedJWT.sign(new ECDSASigner(SIGNING_KEY));
        return signedJWT.serialize();
    }
}