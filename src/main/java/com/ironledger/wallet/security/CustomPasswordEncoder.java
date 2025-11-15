package com.ironledger.wallet.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;

public class CustomPasswordEncoder implements PasswordEncoder {

    private static final int MAX_BYTES = 72;

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(truncate(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return delegate.matches(truncate(rawPassword), encodedPassword);
    }

    private String truncate(CharSequence rawPassword) {
        if (rawPassword == null) return null;

        byte[] bytes = rawPassword.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= MAX_BYTES) return rawPassword.toString();

        byte[] truncated = new byte[MAX_BYTES];
        System.arraycopy(bytes, 0, truncated, 0, MAX_BYTES);

        String result = new String(truncated, StandardCharsets.UTF_8);

        while (!result.isEmpty() && result.charAt(result.length() - 1) == '\uFFFD') {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }
}
