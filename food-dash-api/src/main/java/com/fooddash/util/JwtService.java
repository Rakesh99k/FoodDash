package com.fooddash.util;

import com.fooddash.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	@Value("${security.jwt.secret}")
	private String jwtSecret;

	@Value("${security.jwt.access-token-expiration-ms}")
	private long accessTokenExpirationMs;

	@Value("${security.jwt.refresh-token-expiration-ms}")
	private long refreshTokenExpirationMs;

	public String generateAccessToken(User user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("role", user.getRole().name());
		claims.put("tokenType", "access");
		claims.put("uid", user.getId());
		return buildToken(claims, user.getEmail(), accessTokenExpirationMs);
	}

	public String generateRefreshToken(User user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("tokenType", "refresh");
		claims.put("uid", user.getId());
		return buildToken(claims, user.getEmail(), refreshTokenExpirationMs);
	}

	public String extractEmail(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public Instant extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration).toInstant();
	}

	public boolean isAccessToken(String token) {
		return "access".equals(extractAllClaims(token).get("tokenType"));
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		String email = extractEmail(token);
		return email != null && email.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	public Long extractUserId(String token) {
		return extractClaim(token, claims -> {
			Object uid = claims.get("uid");
			if (uid instanceof Number number) {
				return number.longValue();
			}
			if (uid == null) {
				return null;
			}
			return Long.valueOf(uid.toString());
		});
	}

	private boolean isTokenExpired(String token) {
		return extractClaim(token, Claims::getExpiration).before(new Date());
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
	}

	private String buildToken(Map<String, Object> claims, String subject, long expirationMillis) {
		Instant now = Instant.now();
		return Jwts.builder()
				.claims(claims)
				.subject(subject)
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(expirationMillis)))
				.signWith(signingKey())
				.compact();
	}

	private SecretKey signingKey() {
		byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
