package com.fooddash.dto;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderTrackingResponse {

	private Long orderId;
	private Long deliveryPersonId;
	private String status;
	private Instant etaAt;
	private Map<String, Object> trackingData;
	private Instant updatedAt;
}
