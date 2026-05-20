package com.fooddash.dto;

import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderTrackingUpdateRequest {

	private Double latitude;
	private Double longitude;
	private Instant etaAt;
	private String note;
}
