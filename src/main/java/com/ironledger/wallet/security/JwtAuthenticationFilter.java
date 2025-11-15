package com.ironledger.wallet.security;

import com.ironledger.wallet.entity.User;
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

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

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
            String token = TokenUtils.extractToken(request.getHeader("Authorization"));

            if (token != null) {
                Optional<Claims> claimsOpt = TokenUtils.safeParseClaims(token, jwtProvider.getKey());

                if (claimsOpt.isPresent()) {
                    Claims claims = claimsOpt.get();

                    // Only access tokens allowed here
                    if ("ACCESS".equals(TokenUtils.getTokenType(claims)) &&
                            !TokenUtils.isExpired(claims)) {

                        UUID userId = UUID.fromString(TokenUtils.getUserId(claims));
                        
                        // Try cache first, then database
                        User user = getUserFromCacheOrDatabase(userId);

                        if (user != null) {
                            // Validate passwordVersion match
                            Integer tokenVersion = TokenUtils.getPasswordVersion(claims);
                            if (tokenVersion.equals(user.getPasswordVersion())) {

                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(
                                                user,
                                                null,
                                                null // No roles yet can add UserDetails implementation later
                                        );

                                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(auth);
                            } else {
                                // Password version mismatch - invalidate cache
                                userCache.remove(userId);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("JWT Filter error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Get user from cache if available and not expired, otherwise fetch from database.
     * This significantly reduces database queries for authenticated requests.
     */
    private User getUserFromCacheOrDatabase(UUID userId) {
        CachedUser cached = userCache.get(userId);
        
        // Check cache first
        if (cached != null && !cached.isExpired()) {
            return cached.user;
        }

        // Cache miss or expired - fetch from database
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            userCache.put(userId, new CachedUser(user));
            return user;
        }

        // User not found - remove from cache if present
        userCache.remove(userId);
        return null;
    }
}
