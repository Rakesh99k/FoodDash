package com.fooddash.service;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

	private static final String BLACKLIST_PREFIX = "auth:jwt:blacklist:";

	private final StringRedisTemplate stringRedisTemplate;

	public void blacklistToken(String token, Instant expiresAt) {
		if (token == null || token.isBlank() || expiresAt == null) {
			return;
		}
		Duration ttl = Duration.between(Instant.now(), expiresAt);
		if (ttl.isZero() || ttl.isNegative()) {
			return;
		}
		try {
			stringRedisTemplate.opsForValue().set(key(token), "1", ttl);
		}
		catch (DataAccessException ignored) {
		}
	}

	public boolean isBlacklisted(String token) {
		if (token == null || token.isBlank()) {
			return false;
		}
		try {
			Boolean exists = stringRedisTemplate.hasKey(key(token));
			return Boolean.TRUE.equals(exists);
		}
		catch (DataAccessException ignored) {
			return false;
		}
	}

	private String key(String token) {
		return BLACKLIST_PREFIX + token;
	}
}
