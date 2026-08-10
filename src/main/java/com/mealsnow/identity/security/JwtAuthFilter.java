package com.mealsnow.identity.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // 1. no token → skip auth, let the request continue
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);   // strip "Bearer "

        try {
            Claims claims = jwtService.parseClaims(token);
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(
                    userId,        // principal
                    null,          // credentials — none, token already proved it
                    authorities
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // bad/expired token → leave context empty; chain decides what to do
        }

        filterChain.doFilter(request, response);
    }
}