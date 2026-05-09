package com.fooddash.controller;

import com.fooddash.dto.ApiResponse;
import com.fooddash.dto.ChangePasswordRequest;
import com.fooddash.dto.UpdateProfileRequest;
import com.fooddash.dto.UserResponse;
import com.fooddash.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
		return ResponseEntity.ok(ApiResponse.success("Profile retrieved", userService.getCurrentUserProfile()));
	}

	@PutMapping("/me")
	public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
		UserResponse updated = userService.updateCurrentUserProfile(request);
		return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
	}

	@PostMapping("/me/change-password")
	public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		userService.changeCurrentUserPassword(request);
		return ResponseEntity.ok(ApiResponse.success("Password changed", Map.of("result", "ok")));
	}
}
