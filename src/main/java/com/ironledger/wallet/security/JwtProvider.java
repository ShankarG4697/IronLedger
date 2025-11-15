package com.ironledger.wallet.security;

import com.ironledger.wallet.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtProvider {

    @Getter
    private final Key key;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtProvider(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.access-token-validity}") long accessTokenValidity,
            @Value("${security.jwt.refresh-token-validity}") long refreshTokenValidity
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    // ----------------------------------------------
    // Generate Access Token (Short Lived, Stateless)
    // ----------------------------------------------
    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenValidity);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("role", user.getRole() == 1 ? "USER" : user.getRole() == 0 ? "ADMIN" : "GUEST")
                .claim("token_type", "ACCESS")
                .claim("password_version", user.getPasswordVersion())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ----------------------------------------------
    // Generate Refresh Token (Long Lived)
    // ----------------------------------------------
    public String generateRefreshToken(User user, UUID sessionId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenValidity);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("session_id", sessionId.toString())
                .claim("token_type", "REFRESH")
                .claim("password_version", user.getPasswordVersion())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ----------------------------------------------
    // Validate a JWT token
    // ----------------------------------------------
    public boolean validateToken(String token, Integer currentPasswordVersion) {
        try {
            Claims claims = parseClaims(token);

            // Password version mismatch → invalidates every token issued before password reset
            Integer tokenPasswordVersion = claims.get("password_version", Integer.class);
            if (!tokenPasswordVersion.equals(currentPasswordVersion)) {
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // ----------------------------------------------
    // Extract Claims
    // ----------------------------------------------
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ----------------------------------------------
    // Claim Extractors
    // ----------------------------------------------
    public UUID extractUserId(String token) {
        return UUID.fromString(parseClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String extractTokenType(String token) {
        return parseClaims(token).get("token_type", String.class);
    }

    public UUID extractSessionId(String token) {
        String session = parseClaims(token).get("session_id", String.class);
        return session != null ? UUID.fromString(session) : null;
    }
}
