package com.ironledger.wallet.controller;

import com.ironledger.wallet.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/secure-test")
public class SecureTestController {

    @GetMapping
    public String test(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return "No authentication found (this should not happen if security is configured correctly)";
        }

        User user = (User) authentication.getPrincipal();
        return "JWT OK → Authenticated userId = " + user.getId();
    }
}
