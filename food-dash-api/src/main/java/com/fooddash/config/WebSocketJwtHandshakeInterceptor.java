package com.fooddash.config;

import com.fooddash.service.CustomUserDetailsService;
import com.fooddash.service.TokenBlacklistService;
import com.fooddash.util.JwtService;
import io.jsonwebtoken.JwtException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
@RequiredArgsConstructor
public class WebSocketJwtHandshakeInterceptor implements HandshakeInterceptor {

	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;
	private final TokenBlacklistService tokenBlacklistService;

	@Override
	public boolean beforeHandshake(
			org.springframework.http.server.ServerHttpRequest request,
			org.springframework.http.server.ServerHttpResponse response,
			WebSocketHandler wsHandler,
			Map<String, Object> attributes) {
		String token = extractToken(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
		if (token == null) {
			token = extractTokenFromQuery(request.getURI().getQuery());
		}
		if (token == null || tokenBlacklistService.isBlacklisted(token)) {
			return false;
		}

		try {
			String email = jwtService.extractEmail(token);
			if (email == null || !jwtService.isAccessToken(token)) {
				return false;
			}

			UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
			if (!jwtService.isTokenValid(token, userDetails)) {
				return false;
			}

			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
			attributes.put("principal", authentication);
			return true;
		}
		catch (JwtException | IllegalArgumentException ex) {
			return false;
		}
	}

	@Override
	public void afterHandshake(
			org.springframework.http.server.ServerHttpRequest request,
			org.springframework.http.server.ServerHttpResponse response,
			WebSocketHandler wsHandler,
			Exception exception) {
	}

	private String extractToken(String rawValue) {
		if (rawValue == null || rawValue.isBlank()) {
			return null;
		}
		String value = rawValue.trim();
		if (value.startsWith("Bearer ")) {
			return value.substring(7).trim();
		}
		if (value.startsWith("token=")) {
			return value.substring(6).trim();
		}
		if (value.startsWith("access_token=")) {
			return value.substring(13).trim();
		}
		return null;
	}

	private String extractTokenFromQuery(String query) {
		if (query == null || query.isBlank()) {
			return null;
		}
		for (String part : query.split("&")) {
			String token = extractToken(part);
			if (token != null) {
				return token;
			}
		}
		return null;
	}
}
