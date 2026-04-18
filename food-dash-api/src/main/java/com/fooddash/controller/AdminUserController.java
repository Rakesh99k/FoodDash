package com.fooddash.controller;

import com.fooddash.dto.AdminUpdateUserRequest;
import com.fooddash.dto.ApiResponse;
import com.fooddash.dto.UserResponse;
import com.fooddash.model.Role;
import com.fooddash.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

	private final UserService userService;

	@GetMapping
	public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
			@RequestParam(required = false) Role role, @PageableDefault(size = 20) Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("Users fetched", userService.listUsers(role, pageable)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("User fetched", userService.getUserById(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<ApiResponse<UserResponse>> updateUser(
			@PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
		return ResponseEntity.ok(ApiResponse.success("User updated", userService.updateUserByAdmin(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("User deactivated", userService.deactivateUser(id)));
	}
}
