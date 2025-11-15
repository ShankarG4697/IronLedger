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
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

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
                        Optional<User> userOpt = userRepository.findById(userId);

                        if (userOpt.isPresent()) {
                            User user = userOpt.get();

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
}
