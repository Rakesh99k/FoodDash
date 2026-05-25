package com.fooddash.controller;

import com.fooddash.dto.ApiResponse;
import com.fooddash.dto.OrderTrackingResponse;
import com.fooddash.dto.OrderTrackingUpdateRequest;
import com.fooddash.service.OrderTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderTrackingController {

	private final OrderTrackingService orderTrackingService;

	@GetMapping("/{id}/tracking")
	public ResponseEntity<ApiResponse<OrderTrackingResponse>> getTracking(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Order tracking fetched", orderTrackingService.getTracking(id)));
	}

	@PatchMapping("/{id}/tracking")
	public ResponseEntity<ApiResponse<OrderTrackingResponse>> updateTracking(
			@PathVariable Long id,
			@RequestBody OrderTrackingUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Order tracking updated", orderTrackingService.updateTracking(id, request)));
	}
}
