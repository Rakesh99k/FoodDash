package com.fooddash.dto;

import com.fooddash.model.RestaurantStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRestaurantRequest {

	private Long ownerId;

	@NotBlank
	@Size(max = 150)
	private String name;

	private String description;

	@NotBlank
	@Size(max = 255)
	private String address;

	@NotNull
	private Double latitude;

	@NotNull
	private Double longitude;

	@NotBlank
	@Size(max = 100)
	private String cuisineType;

	private Map<String, Object> openingHours;

	private RestaurantStatus status;
}
