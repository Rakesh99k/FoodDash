package com.fooddash.service;

import com.fooddash.dto.HealthResponseDto;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthService {

	private final Environment environment;

	public HealthResponseDto getHealth() {
		String[] activeProfiles = environment.getActiveProfiles();
		String profile = activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
		log.info("Health check requested for profile {}", profile);

		return HealthResponseDto.builder()
				.application(environment.getProperty("spring.application.name", "food-dash-api"))
				.environment(profile)
				.timestamp(Instant.now().toString())
				.build();
	}
}
