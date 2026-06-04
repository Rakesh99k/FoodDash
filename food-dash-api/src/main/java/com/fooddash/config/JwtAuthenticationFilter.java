package com.fooddash.config;

import com.fooddash.service.CustomUserDetailsService;
import com.fooddash.service.TokenBlacklistService;
import com.fooddash.util.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;
	private final TokenBlacklistService tokenBlacklistService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			try {
				filterChain.doFilter(request, response);
			}
			finally {
				MDC.remove("userId");
			}
			return;
		}

		String token = authHeader.substring(7);
		if (tokenBlacklistService.isBlacklisted(token)) {
			SecurityContextHolder.clearContext();
			try {
				filterChain.doFilter(request, response);
			}
			finally {
				MDC.remove("userId");
			}
			return;
		}
		try {
			String email = jwtService.extractEmail(token);
			if (email != null
					&& SecurityContextHolder.getContext().getAuthentication() == null
					&& jwtService.isAccessToken(token)) {
				UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
				if (userDetails.isEnabled() && jwtService.isTokenValid(token, userDetails)) {
					UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authenticationToken);
					Long userId = jwtService.extractUserId(token);
					if (userId != null) {
						MDC.put("userId", userId.toString());
					}
				}
			}
		}
		catch (JwtException ignored) {
			SecurityContextHolder.clearContext();
		}

		try {
			filterChain.doFilter(request, response);
		}
		finally {
			MDC.remove("userId");
		}
	}
}
