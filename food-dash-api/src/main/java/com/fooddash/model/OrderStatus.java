package com.fooddash.model;

public enum OrderStatus {
	PENDING("pending"),
	CONFIRMED("confirmed"),
	PREPARING("preparing"),
	READY_FOR_PICKUP("ready_for_pickup"),
	OUT_FOR_DELIVERY("out_for_delivery"),
	DELIVERED("delivered"),
	CANCELLED("cancelled");

	private final String dbValue;

	OrderStatus(String dbValue) {
		this.dbValue = dbValue;
	}

	public String getDbValue() {
		return dbValue;
	}

	public static OrderStatus fromDbValue(String value) {
		for (OrderStatus status : values()) {
			if (status.dbValue.equalsIgnoreCase(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Unknown order status: " + value);
	}
}
