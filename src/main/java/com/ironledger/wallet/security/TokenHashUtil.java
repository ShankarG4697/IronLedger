package com.ironledger.wallet.security;

import lombok.experimental.UtilityClass;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility class for hashing JWT tokens for storage.
 * Uses SHA-256 instead of BCrypt for performance.
 * BCrypt is designed for passwords (slow by design), not tokens.
 */
@UtilityClass
public class TokenHashUtil {

    private static final String SHA_256 = "SHA-256";

    /**
     * Hash a JWT token using SHA-256.
     * This is much faster than BCrypt and appropriate for tokens.
     *
     * @param token the JWT token string
     * @return hex-encoded SHA-256 hash
     */
    public static String hashToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or blank");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verify if a token matches the stored hash.
     *
     * @param token      the raw JWT token
     * @param storedHash the stored SHA-256 hash
     * @return true if matches, false otherwise
     */
    public static boolean verifyToken(String token, String storedHash) {
        if (token == null || storedHash == null) {
            return false;
        }

        String computedHash = hashToken(token);
        return computedHash.equals(storedHash);
    }
}
