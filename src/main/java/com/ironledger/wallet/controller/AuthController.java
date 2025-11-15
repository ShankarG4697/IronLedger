package com.ironledger.wallet.controller;

import com.ironledger.wallet.dto.*;
import com.ironledger.wallet.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    @Autowired
    private final AuthService authService;

    // ---------------------------------------------------------------------
    // SIGNUP
    // ---------------------------------------------------------------------
    @PostMapping("/signup")
    public SignupResponse signup(@RequestBody SignupRequest signupRequest, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String userAgent = getUserAgent(request);

        return authService.signup(signupRequest, clientIp, userAgent);
    }

    // ---------------------------------------------------------------------
    // LOGIN
    // ---------------------------------------------------------------------
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String userAgent = getUserAgent(request);

        return authService.login(loginRequest, clientIp, userAgent);
    }

    // ---------------------------------------------------------------------
    // LOGOUT
    // ---------------------------------------------------------------------
    @PostMapping("/logout/{sessionId}")
    public String logout(@PathVariable String sessionId) {
        return authService.logout(java.util.UUID.fromString(sessionId));
    }

    // ---------------------------------------------------------------------
    // LOGOUT
    // ---------------------------------------------------------------------
    @PostMapping("/reset-password")
    public ResetPassword resetPassword(@RequestBody ResetPasswordRequest req, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        String userAgent = getUserAgent(request);

        return authService.resetPassword(req, clientIp, userAgent);
    }
    // ---------------------------------------------------------------------
    // Refresh Access Token
    // ---------------------------------------------------------------------
    @PostMapping("/refresh")
    public TokenPair refresh(@RequestBody RefreshRequest req) {
        return authService.refresh(req.getRefreshToken());
    }

    // =====================================================================
    // PRIVATE HELPERS
    // =====================================================================

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // If behind reverse proxy, use first IP
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null ? ua : "unknown";
    }
}
