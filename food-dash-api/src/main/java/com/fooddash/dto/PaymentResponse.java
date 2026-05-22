package com.fooddash.dto;

import com.fooddash.model.PaymentMethod;
import com.fooddash.model.PaymentProvider;
import com.fooddash.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {

	private Long paymentId;
	private Long orderId;
	private BigDecimal amount;
	private String currency;
	private PaymentProvider provider;
	private PaymentMethod method;
	private PaymentStatus status;
	private String providerOrderId;
	private String providerPaymentId;
	private String clientSecret;
	private Instant paidAt;
}
