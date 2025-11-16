package com.ironledger.wallet.security;

import com.ironledger.wallet.entity.AuthSession;
import com.ironledger.wallet.entity.User;
import com.ironledger.wallet.repository.AuthSessionRepository;
import com.ironledger.wallet.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String SESSION_ID_PARAM = "sessionId";
    private static final String MSG_USER_SESSION_INVALID = "User Session is expired or invalid";
    private static final String MSG_TOKEN_INVALID_OR_EXPIRED = "Token is expired or invalid";
    private static final String MSG_TOKEN_INVALID = "Invalid token";

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;

    // Simple cache to avoid repeated DB queries for the same user within a short time window
    private final Map<UUID, CachedUser> userCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_SECONDS = 60; // Cache for 1 minute

    private static class CachedUser {
        final User user;
        final Instant cachedAt;

        CachedUser(User user) {
            this.user = user;
            this.cachedAt = Instant.now();
        }

        boolean isExpired() {
            return Instant.now().isAfter(cachedAt.plusSeconds(CACHE_TTL_SECONDS));
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = TokenUtils.extractToken(request.getHeader(AUTH_HEADER));
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            Optional<Claims> claimsOpt = TokenUtils.safeParseClaims(token, jwtProvider.getKey());
            if (claimsOpt.isEmpty()) {
                respondUnauthorized(response, "JWT Filter: failed to parse token", MSG_TOKEN_INVALID);
                filterChain.doFilter(request, response);
                return;
            }

            Claims claims = claimsOpt.get();
            if (!isValidAccessToken(claims)) {
                respondUnauthorized(response, "JWT Filter: token is expired or invalid", MSG_TOKEN_INVALID_OR_EXPIRED);
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(TokenUtils.getUserId(claims));
            String sessionIdParam = request.getParameter(SESSION_ID_PARAM);
            if (sessionIdParam == null) {
                respondUnauthorized(response, "JWT Filter: sessionId parameter is missing", MSG_USER_SESSION_INVALID);
                filterChain.doFilter(request, response);
                return;
            }

            UUID sessionId = UUID.fromString(sessionIdParam);
            boolean userSessionActive = authSessionRepository.existsActiveSession(userId, sessionId);
            if (!userSessionActive) {
                respondUnauthorized(response, "JWT Filter: User session is invalid", MSG_USER_SESSION_INVALID);
                filterChain.doFilter(request, response);
                return;
            }

            Optional<User> userOpt = getUserFromCacheOrDatabase(userId);
            if (userOpt.isEmpty()) {
                respondUnauthorized(response, "JWT Filter: User not found", MSG_USER_SESSION_INVALID);
                filterChain.doFilter(request, response);
                return;
            }

            User user = userOpt.get();
            // Validate passwordVersion match
            Integer tokenVersion = TokenUtils.getPasswordVersion(claims);
            if (!tokenVersion.equals(user.getPasswordVersion())) {
                // Password version mismatch - invalidate cache
                userCache.remove(userId);
                respondUnauthorized(response, "JWT Filter: password version mismatch", MSG_TOKEN_INVALID_OR_EXPIRED);
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken auth = buildAuthentication(user, request);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (Exception e) {
            log.warn("JWT Filter error: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(MSG_TOKEN_INVALID);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidAccessToken(Claims claims) {
        return "ACCESS".equals(TokenUtils.getTokenType(claims)) &&
                !TokenUtils.isExpired(claims);
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(User user,
                                                                    HttpServletRequest request) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        null // No roles yet; can add UserDetails implementation later
                );
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        return auth;
    }

    private void respondUnauthorized(HttpServletResponse response,
                                     String logMessage,
                                     String clientMessage) throws IOException {
        log.warn(logMessage);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write(clientMessage);
        SecurityContextHolder.clearContext();
    }

    /**
     * Get user from cache if available and not expired, otherwise fetch from database.
     * This significantly reduces database queries for authenticated requests.
     */
    private Optional<User> getUserFromCacheOrDatabase(UUID userId) {
        CachedUser cached = userCache.get(userId);
        // Check cache first
        if (cached != null && !cached.isExpired()) {
            return Optional.of(cached.user);
        }

        // Cache miss or expired - fetch from database
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            userCache.put(userId, new CachedUser(userOpt.get()));
            return userOpt;
        }

        // User isn't found - remove from cache if present
        userCache.remove(userId);
        return Optional.empty();
    }
}
