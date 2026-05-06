package com.fooddash.controller;

import com.fooddash.dto.ApiResponse;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/secure")
public class RoleProtectedController {

	@GetMapping("/customer")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ApiResponse<Map<String, String>> customerOnly() {
		return ApiResponse.success("Customer access granted", Map.of("scope", "customer"));
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<Map<String, String>> adminOnly() {
		return ApiResponse.success("Admin access granted", Map.of("scope", "admin"));
	}
}
