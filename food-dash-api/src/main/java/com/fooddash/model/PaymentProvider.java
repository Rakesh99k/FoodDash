package com.fooddash.model;

public enum PaymentProvider {
	STRIPE("stripe"),
	RAZORPAY("razorpay");

	private final String dbValue;

	PaymentProvider(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}

	public static PaymentProvider fromDbValue(String value) {
		for (PaymentProvider provider : values()) {
			if (provider.dbValue.equalsIgnoreCase(value)) {
				return provider;
			}
		}
		throw new IllegalArgumentException("Unknown payment provider: " + value);
	}
}
