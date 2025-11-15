package com.ironledger.wallet.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenHashUtilTest {

    @Test
    void testHashToken_returnsConsistentHash() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        
        String hash1 = TokenHashUtil.hashToken(token);
        String hash2 = TokenHashUtil.hashToken(token);
        
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertEquals(hash1, hash2, "Same token should produce same hash");
    }

    @Test
    void testHashToken_differentTokensProduceDifferentHashes() {
        String token1 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test1.token";
        String token2 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test2.token";
        
        String hash1 = TokenHashUtil.hashToken(token1);
        String hash2 = TokenHashUtil.hashToken(token2);
        
        assertNotEquals(hash1, hash2, "Different tokens should produce different hashes");
    }

    @Test
    void testHashToken_nullToken_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TokenHashUtil.hashToken(null));
    }

    @Test
    void testHashToken_blankToken_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> TokenHashUtil.hashToken(""));
        assertThrows(IllegalArgumentException.class, () -> TokenHashUtil.hashToken("   "));
    }

    @Test
    void testHashToken_returnsHexString() {
        String token = "test.token.value";
        String hash = TokenHashUtil.hashToken(token);
        
        // SHA-256 hash should be 64 hex characters (32 bytes * 2)
        assertEquals(64, hash.length());
        assertTrue(hash.matches("^[0-9a-f]+$"), "Hash should be lowercase hexadecimal");
    }

    @Test
    void testVerifyToken_validToken_returnsTrue() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        String hash = TokenHashUtil.hashToken(token);
        
        assertTrue(TokenHashUtil.verifyToken(token, hash));
    }

    @Test
    void testVerifyToken_invalidToken_returnsFalse() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        String wrongToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.wrong.token";
        String hash = TokenHashUtil.hashToken(token);
        
        assertFalse(TokenHashUtil.verifyToken(wrongToken, hash));
    }

    @Test
    void testVerifyToken_nullToken_returnsFalse() {
        String hash = TokenHashUtil.hashToken("test.token");
        assertFalse(TokenHashUtil.verifyToken(null, hash));
    }

    @Test
    void testVerifyToken_nullHash_returnsFalse() {
        assertFalse(TokenHashUtil.verifyToken("test.token", null));
    }

    @Test
    void testPerformance_hashingIsFast() {
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        
        // Warm up
        for (int i = 0; i < 100; i++) {
            TokenHashUtil.hashToken(token);
        }
        
        // Measure
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            TokenHashUtil.hashToken(token);
        }
        long end = System.nanoTime();
        
        long avgTimeMs = (end - start) / 1_000_000 / 1000;
        
        // Should be much faster than BCrypt (which takes ~100ms)
        // SHA-256 should complete 1000 iterations in under 50ms
        assertTrue(avgTimeMs < 50, 
            "1000 hashes should complete in under 50ms, took: " + avgTimeMs + "ms");
    }
}
