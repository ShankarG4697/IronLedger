package com.ironledger.wallet.service;

import com.ironledger.wallet.dto.*;
import com.ironledger.wallet.entity.AuthSession;
import com.ironledger.wallet.entity.LoginAudit;
import com.ironledger.wallet.entity.User;
import com.ironledger.wallet.exception.AccountLockedException;
import com.ironledger.wallet.exception.DuplicateResourceException;
import com.ironledger.wallet.exception.InvalidCredentialsException;
import com.ironledger.wallet.exception.ResourceNotFoundException;
import com.ironledger.wallet.repository.AuthSessionRepository;
import com.ironledger.wallet.repository.LoginAuditRepository;
import com.ironledger.wallet.repository.UserRepository;
import com.ironledger.wallet.security.JwtProvider;
import com.ironledger.wallet.security.TokenHashUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final ZoneId SG_ZONE = ZoneId.of("Asia/Singapore");
    private static final String SESSION_ALREADY_REVOKED_MESSAGE = "Session already revoked";
    private static final String LOGOUT_SUCCESSFUL_MESSAGE = "Logout successful";
    private static final String SESSION_NOT_FOUND_MESSAGE = "Session not found";

    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final LoginAuditRepository loginAuditRepository;

    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    // -------------------------------------------------------------------------
    // SIGNUP
    // -------------------------------------------------------------------------
    @Transactional
    public SignupResponse signup(SignupRequest signupRequest, String ipAddress, String userAgent) {

        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        String email = signupRequest.getEmail().toLowerCase();
        String safePassword = truncatePassword(signupRequest.getPassword());

        User user = User.builder()
                .email(email)
                .fullName(signupRequest.getFullName())
                .phone(signupRequest.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(safePassword))
                .passwordVersion(1)
                .status(1)   // Active
                .role(1)     // User
                .build();

        userRepository.save(user);

        user = userRepository.findById(user.getId()).orElseThrow();

        logAttempt(user.getId(), true, "User Created!", ipAddress, userAgent);

        return new SignupResponse(
                user.getId(), user.getFullName(), user.getPhone(), user.getEmail(),
                user.getRole() == 1 ? "USER" : "ADMIN",
                user.getStatus() == 1 ? "ACTIVE" : "LOCKED"
        );
    }

    // -------------------------------------------------------------------------
    // LOGIN
    // -------------------------------------------------------------------------
    @Transactional
    public AuthResponse login(LoginRequest loginRequest, String ipAddress, String userAgent) {

        Optional<User> optUser = userRepository.findByEmail(loginRequest.getEmail().toLowerCase());

        if (optUser.isEmpty()) {
            logAttempt(null, false, "USER_NOT_FOUND", ipAddress, userAgent);
            throw new ResourceNotFoundException("User not found");
        }

        User user = optUser.get();

        String incomingPassword = truncatePassword(loginRequest.getPassword());

        if (!passwordEncoder.matches(incomingPassword, user.getPasswordHash())) {
            logAttempt(user.getId(), false, "INVALID_PASSWORD", ipAddress, userAgent);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (user.getStatus() != 1) {
            logAttempt(user.getId(), false, "USER_LOCKED", ipAddress, userAgent);
            throw new AccountLockedException("User is locked or disabled");
        }

        logAttempt(user.getId(), true, "Login Successful", ipAddress, userAgent);

        // --- STEP 1: Create session WITHOUT refresh token hash ---
        // Create session
        AuthSession session = AuthSession.builder()
                .userId(user.getId())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(OffsetDateTime.now(SG_ZONE).plusDays(14))
                .build();

        authSessionRepository.save(session);

        // --- STEP 2: Generate tokens now that sessionId exists ---
        UUID sessionId = session.getId();
        String refreshToken = jwtProvider.generateRefreshToken(user, sessionId);
        String accessToken = jwtProvider.generateAccessToken(user);

        // --- STEP 3: Update session with refresh token hash (SHA-256, not BCrypt) ---
        session.setRefreshTokenHash(TokenHashUtil.hashToken(refreshToken));
        authSessionRepository.save(session);

        return new AuthResponse(new UserDto(
                user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole() == 1 ? "USER" : "ADMIN",
                user.getStatus() == 1 ? "ACTIVE" : "LOCKED",
                sessionId
        ), accessToken, refreshToken);
    }

    // -------------------------------------------------------------------------
    // REFRESH TOKEN
    // -------------------------------------------------------------------------
    @Transactional
    public TokenPair refresh(String rawRefreshToken) {

        UUID userId = jwtProvider.extractUserId(rawRefreshToken);
        UUID sessionId = jwtProvider.extractSessionId(rawRefreshToken);

        log.debug("Refreshing token for user {} and session {}", userId, sessionId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        AuthSession session = authSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        if (session.getRevokedAt() != null) {
            throw new InvalidCredentialsException("Session revoked");
        }

        if (session.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new InvalidCredentialsException("Refresh token expired");
        }

        if (!TokenHashUtil.verifyToken(rawRefreshToken, session.getRefreshTokenHash())) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        // Token rotation - generate new tokens and update stored hash
        String newAccessToken = jwtProvider.generateAccessToken(user);
        String newRefreshToken = jwtProvider.generateRefreshToken(user, session.getId());

        session.setRefreshTokenHash(TokenHashUtil.hashToken(newRefreshToken));

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    // -------------------------------------------------------------------------
    // LOGOUT
    // -------------------------------------------------------------------------
    @Transactional
    public String logout(UUID sessionId) {
        return authSessionRepository.findById(sessionId)
                .map(this::buildLogoutMessage)
                .orElse(SESSION_NOT_FOUND_MESSAGE);
    }

    // -------------------------------------------------------------------------
    // RESET PASSWORD
    // -------------------------------------------------------------------------
    @Transactional
    public ResetPassword resetPassword(ResetPasswordRequest req, String ipAddress, String userAgent) {

        User user = userRepository.findById(req.getUserId()).orElse(null);
        if (user == null) {
            logAttempt(null, false, "USER_NOT_FOUND", ipAddress, userAgent);
            throw new IllegalArgumentException("User not found");
        }

        AuthSession session = authSessionRepository.findById(req.getSessionId()).orElse(null);
        if (session == null || !req.getUserId().equals(session.getUserId()) || session.getRevokedAt() != null) {
            logAttempt(user.getId(), false, "INVALID_OR_REVOKED_SESSION", ipAddress, userAgent);
            throw new ResourceNotFoundException("Invalid or revoked session");
        }

        String safePassword = truncatePassword(req.getPassword());

        user.setPasswordHash(passwordEncoder.encode(safePassword));
        user.setPasswordVersion(user.getPasswordVersion() + 1);

        userRepository.save(user);

        // Invalidate all sessions
        authSessionRepository.revokeAllSessionsForUser(req.getUserId());

        logAttempt(user.getId(), true, "Password reset successful", ipAddress, userAgent);

        return new ResetPassword(
                user.getId(), user.getFullName(), user.getEmail()
        );
    }

    // -------------------------------------------------------------------------
    // PRIVATE FUNCTIONS
    // -------------------------------------------------------------------------
    private void logAttempt(UUID userId, boolean success, String reason, String ip, String userAgent) {
        LoginAudit audit = LoginAudit.builder()
                .userId(userId)
                .success(success)
                .reason(reason)
                .ipAddress(ip)
                .userAgent(userAgent)
                .build();

        loginAuditRepository.save(audit);
    }

    private String truncatePassword(String pwd) {
        if (pwd == null) return null;

        byte[] bytes = pwd.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= MAX_BCRYPT_PASSWORD_BYTES) {
            return pwd;
        }

        byte[] truncated = new byte[MAX_BCRYPT_PASSWORD_BYTES];
        System.arraycopy(bytes, 0, truncated, 0, MAX_BCRYPT_PASSWORD_BYTES);

        String result = new String(truncated, StandardCharsets.UTF_8);

        while (!result.isEmpty() && result.charAt(result.length() - 1) == '\uFFFD') {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private String buildLogoutMessage(AuthSession session) {
        if (session.getRevokedAt() != null) {
            return SESSION_ALREADY_REVOKED_MESSAGE;
        }

        session.setRevokedAt(OffsetDateTime.now(ZoneOffset.UTC));
        session = authSessionRepository.save(session);
        return LOGOUT_SUCCESSFUL_MESSAGE;
    }
}
