package com.fooddash.dto;

import com.fooddash.model.PaymentMethod;
import com.fooddash.model.PaymentProvider;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentInitiateRequest {

	@NotNull
	private PaymentProvider provider;

	@NotNull
	private PaymentMethod method;

	private String currency;
}
