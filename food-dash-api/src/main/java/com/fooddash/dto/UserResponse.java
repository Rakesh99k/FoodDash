package com.fooddash.dto;

import com.fooddash.model.Role;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

	private Long id;
	private String email;
	private String fullName;
	private String phone;
	private Role role;
	private boolean active;
	private Instant createdAt;
	private Instant updatedAt;
}
