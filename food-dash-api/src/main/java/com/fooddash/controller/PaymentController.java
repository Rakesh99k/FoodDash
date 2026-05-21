package com.fooddash.controller;

import com.fooddash.dto.ApiResponse;
import com.fooddash.dto.PaymentInitiateRequest;
import com.fooddash.dto.PaymentResponse;
import com.fooddash.service.PaymentService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService paymentService;

	@PostMapping("/orders/{orderId}/initiate")
	public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
			@PathVariable Long orderId,
			@Valid @RequestBody PaymentInitiateRequest request) {
		PaymentResponse response = paymentService.initiatePayment(orderId, request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Payment initiated", response));
	}

	@PostMapping("/webhooks/stripe")
	public ResponseEntity<ApiResponse<Map<String, String>>> stripeWebhook(
			@RequestHeader(value = "Stripe-Signature", required = false) String signature,
			@RequestBody String payload) {
		paymentService.handleStripeWebhook(signature, payload);
		return ResponseEntity.ok(ApiResponse.success("Webhook processed", Map.of("status", "ok")));
	}

	@PostMapping("/webhooks/razorpay")
	public ResponseEntity<ApiResponse<Map<String, String>>> razorpayWebhook(
			@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
			@RequestHeader(value = "x-razorpay-event-id", required = false) String eventId,
			@RequestBody String payload) {
		paymentService.handleRazorpayWebhook(signature, eventId, payload);
		return ResponseEntity.ok(ApiResponse.success("Webhook processed", Map.of("status", "ok")));
	}
}
