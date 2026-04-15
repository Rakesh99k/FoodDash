package com.fooddash.dto;

import com.fooddash.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserRequest {

	@Email
	@NotBlank
	private String email;

	@NotBlank
	@Size(max = 120)
	private String fullName;

	@Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Phone must be a valid contact number")
	private String phone;

	private Role role;

	private Boolean active;
}
