package com.fooddash.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class HealthResponseDto {

	private final String application;
	private final String environment;
	private final String timestamp;
}
