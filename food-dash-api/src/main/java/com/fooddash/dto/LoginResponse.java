package com.fooddash.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

	private String accessToken;
	private String refreshToken;
	private UserResponse user;
}
