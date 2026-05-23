package com.fooddash.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionOtpCacheService {

	private static final String OTP_PREFIX = "auth:otp:";
	private static final String SESSION_PREFIX = "auth:session:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public void cacheOtp(String key, String otpCode, Duration ttl) {
		putValue(OTP_PREFIX + key, otpCode, ttl);
	}

	public String getOtp(String key) {
		return getValue(OTP_PREFIX + key);
	}

	public void removeOtp(String key) {
		deleteKey(OTP_PREFIX + key);
	}

	public void cacheSession(String sessionId, Map<String, Object> payload, Duration ttl) {
		try {
			putValue(SESSION_PREFIX + sessionId, objectMapper.writeValueAsString(payload), ttl);
		}
		catch (Exception ignored) {
		}
	}

	public Map<String, Object> getSession(String sessionId) {
		String raw = getValue(SESSION_PREFIX + sessionId);
		if (raw == null) {
			return Map.of();
		}
		try {
			return objectMapper.readValue(raw, new TypeReference<>() {
			});
		}
		catch (Exception ignored) {
			return Map.of();
		}
	}

	public void removeSession(String sessionId) {
		deleteKey(SESSION_PREFIX + sessionId);
	}

	private void putValue(String key, String value, Duration ttl) {
		if (key == null || key.isBlank() || value == null || ttl == null || ttl.isNegative() || ttl.isZero()) {
			return;
		}
		try {
			stringRedisTemplate.opsForValue().set(key, value, ttl);
		}
		catch (DataAccessException ignored) {
		}
	}

	private String getValue(String key) {
		try {
			return stringRedisTemplate.opsForValue().get(key);
		}
		catch (DataAccessException ignored) {
			return null;
		}
	}

	private void deleteKey(String key) {
		try {
			stringRedisTemplate.delete(key);
		}
		catch (DataAccessException ignored) {
		}
	}
}
