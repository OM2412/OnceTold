package com.oncetold.oncetold.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import com.oncetold.oncetold.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * JWT utility using Nimbus JOSE+JWT (already on classpath via
 * spring-security-oauth2-jose — no extra dependency needed).
 * Signs tokens with HMAC-SHA256 (HS256).
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiryMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiry-ms}") long expiryMs) {
        // Pad/trim to exactly 32 bytes for HS256
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.expiryMs = expiryMs;
    }

    /** Generate a signed JWT token for the given user. */
    public String generateToken(User user) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getEmail())
                    .claim("userId", user.getId())
                    .claim("role", user.getRole().name())
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusMillis(expiryMs)))
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claims
            );
            signedJWT.sign(new MACSigner(secretKey));
            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    /** Parse and verify a JWT token; returns the claims set. */
    public JWTClaimsSet parseToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(secretKey);

            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid JWT signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            if (claims.getExpirationTime().before(Date.from(Instant.now()))) {
                throw new RuntimeException("JWT token has expired");
            }

            return claims;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JWT token: " + e.getMessage(), e);
        }
    }

    public String extractEmail(String token) {
        return parseToken(token).getSubject();
    }

    public String extractRole(String token) {
        try {
            return parseToken(token).getStringClaim("role");
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract role from token", e);
        }
    }
}
