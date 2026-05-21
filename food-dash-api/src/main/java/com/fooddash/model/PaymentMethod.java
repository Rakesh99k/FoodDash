package com.fooddash.model;

public enum PaymentMethod {
	CARD("card"),
	CASH("cash"),
	UPI("upi"),
	WALLET("wallet");

	private final String dbValue;

	PaymentMethod(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}

	public static PaymentMethod fromDbValue(String value) {
		for (PaymentMethod method : values()) {
			if (method.dbValue.equalsIgnoreCase(value)) {
				return method;
			}
		}
		throw new IllegalArgumentException("Unknown payment method: " + value);
	}
}
