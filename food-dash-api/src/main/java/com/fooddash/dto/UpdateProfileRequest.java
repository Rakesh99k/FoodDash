package com.fooddash.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

	@NotBlank
	@Size(max = 120)
	private String fullName;

	@Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone must be a valid contact number")
	private String phone;
}
