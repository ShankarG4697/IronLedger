package com.ironledger.wallet.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomPasswordEncoderTest {

    private CustomPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new CustomPasswordEncoder();
    }

    @Test
    void testEncode_shortPassword() {
        String password = "password123";
        String encoded = encoder.encode(password);
        
        assertNotNull(encoded);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"), 
            "Should be BCrypt hash");
    }

    @Test
    void testMatches_correctPassword() {
        String password = "mySecurePassword123!";
        String encoded = encoder.encode(password);
        
        assertTrue(encoder.matches(password, encoded));
    }

    @Test
    void testMatches_incorrectPassword() {
        String password = "mySecurePassword123!";
        String wrongPassword = "wrongPassword";
        String encoded = encoder.encode(password);
        
        assertFalse(encoder.matches(wrongPassword, encoded));
    }

    @Test
    void testTruncate_longPassword() {
        // Create a password longer than 72 bytes
        String longPassword = "a".repeat(100);
        String encoded = encoder.encode(longPassword);
        
        assertNotNull(encoded);
        // Should still work - password gets truncated
        assertTrue(encoder.matches(longPassword, encoded));
    }

    @Test
    void testTruncate_exactlyMaxBytes() {
        // Exactly 72 bytes
        String password = "a".repeat(72);
        String encoded = encoder.encode(password);
        
        assertNotNull(encoded);
        assertTrue(encoder.matches(password, encoded));
    }

    @Test
    void testEncode_multiByteCharacters() {
        // Test with Unicode characters (multi-byte UTF-8)
        String password = "пароль密码🔒";
        String encoded = encoder.encode(password);
        
        assertNotNull(encoded);
        assertTrue(encoder.matches(password, encoded));
    }

    @Test
    void testStaticDelegate_reuseAcrossInstances() {
        CustomPasswordEncoder encoder1 = new CustomPasswordEncoder();
        CustomPasswordEncoder encoder2 = new CustomPasswordEncoder();
        
        String password = "testPassword123";
        
        // Encode with first encoder
        String encoded = encoder1.encode(password);
        
        // Verify with second encoder should work due to static delegate
        assertTrue(encoder2.matches(password, encoded), 
            "Static BCrypt delegate should be reused across instances");
    }

    @Test
    void testEncode_producesUniqueHashes() {
        String password = "samePassword";
        
        // BCrypt should produce different hashes each time due to salt
        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);
        
        assertNotEquals(hash1, hash2, "BCrypt should produce unique hashes with different salts");
        
        // But both should match the original password
        assertTrue(encoder.matches(password, hash1));
        assertTrue(encoder.matches(password, hash2));
    }
}
