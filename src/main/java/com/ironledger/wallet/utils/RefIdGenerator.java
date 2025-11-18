package com.ironledger.wallet.utils;

import com.github.f4b6a3.ulid.Ulid;

public final class RefIdGenerator {

    // Prevent instantiation of utility class
    private RefIdGenerator() {
        // Intentionally empty
    }

    /**
     * Generates a reference identifier using ULID in fast mode.
     *
     * @return a newly generated ULID-based reference ID as a String
     */
    public static String generateRefId() {
        return Ulid.fast().toString();
    }
}
