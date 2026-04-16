package com.fooddash.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMenuItemRequest {

	@NotBlank
	@Size(max = 150)
	private String name;

	private String description;

	@NotNull
	@DecimalMin(value = "0.0", inclusive = true)
	private BigDecimal price;

	@NotBlank
	@Size(max = 100)
	private String category;

	private String imageUrl;

	private Boolean available;
}
