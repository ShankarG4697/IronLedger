package com.ironledger.wallet.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class TokenUtils {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Extract token string from the Authorization header.
     *
     * @param authHeader the Authorization header value
     * @return token string or null
     */
    public static String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * Check if JWT is expired based on claims.
     *
     * @param claims parsed JWT claims
     * @return true if expired
     */
    public static boolean isExpired(Claims claims) {
        return claims.getExpiration() == null ||
                claims.getExpiration().toInstant().isBefore(java.time.Instant.now());
    }

    /**
     * Check the token type (ACCESS or REFRESH).
     *
     * @param claims parsed JWT claims
     * @return "ACCESS", "REFRESH", or null
     */
    public static String getTokenType(Claims claims) {
        return claims.get("token_type", String.class);
    }

    /**
     * Safely parse claims without throwing exceptions.
     *
     * @param token JWT string
     * @param key   signing key (same key used in JwtProvider)
     * @return Optional<Claims>
     */
    public static Optional<Claims> safeParseClaims(String token, java.security.Key key) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Extract user ID safely from claims.
     *
     * @param claims parsed claims
     * @return userId or null
     */
    public static String getUserId(Claims claims) {
        return claims.getSubject();
    }

    /**
     * Extract session ID from claims (only refresh tokens).
     */
    public static String getSessionId(Claims claims) {
        return claims.get("session_id", String.class);
    }

    /**
     * Extract the password version from claims.
     */
    public static Integer getPasswordVersion(Claims claims) {
        return claims.get("password_version", Integer.class);
    }
}