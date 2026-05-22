package com.fooddash.model;

public enum PaymentStatus {
	PENDING("pending"),
	AUTHORIZED("authorized"),
	PAID("paid"),
	FAILED("failed"),
	REFUNDED("refunded");

	private final String dbValue;

	PaymentStatus(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}

	public static PaymentStatus fromDbValue(String value) {
		for (PaymentStatus status : values()) {
			if (status.dbValue.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown payment status: " + value);
	}
}
