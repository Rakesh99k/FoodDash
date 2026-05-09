package com.fooddash.util;

import com.fooddash.dto.UserResponse;
import com.fooddash.model.User;

public final class UserMapper {

	private UserMapper() {
	}

	public static UserResponse toResponse(User user) {
		return UserResponse.builder()
				.id(user.getId())
				.email(user.getEmail())
				.fullName(user.getFullName())
				.phone(user.getPhone())
				.role(user.getRole())
				.active(user.isActive())
				.createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt())
				.build();
	}
}
