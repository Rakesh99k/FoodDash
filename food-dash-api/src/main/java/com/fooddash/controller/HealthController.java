package com.fooddash.controller;

import com.fooddash.dto.ApiResponse;
import com.fooddash.dto.HealthResponseDto;
import com.fooddash.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

	private final HealthService healthService;

	@GetMapping("/health")
	public ResponseEntity<ApiResponse<HealthResponseDto>> health() {
		return ResponseEntity.ok(ApiResponse.success("Service is healthy", healthService.getHealth()));
	}
}
