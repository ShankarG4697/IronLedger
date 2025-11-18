package com.ironledger.wallet.utils;

import com.ironledger.wallet.entity.User;
import org.springframework.security.core.Authentication;

import java.util.UUID;

public class AuthenticationUtils {
    private AuthenticationUtils() {}

    public static UUID resolveUserIdFromAuthentication(Authentication auth){
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Unauthenticated request");
        }

        if (auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }

        throw new IllegalStateException("Unable to resolve userId from principal");
    }
}
