package com.fooddash.controller;

import com.fooddash.dto.ApiResponse;
import com.fooddash.dto.LoginRequest;
import com.fooddash.dto.LoginResponse;
import com.fooddash.dto.RegisterRequest;
import com.fooddash.dto.UserResponse;
import com.fooddash.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
		UserResponse user = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User registered", user));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		LoginResponse loginResponse = authService.login(request);
		return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
	}
}
