package com.fooddash.dto;

import com.fooddash.util.ApiStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ApiResponse<T> {

	private final String status;
	private final T data;
	private final String message;

	public static <T> ApiResponse<T> success(String message, T data) {
		return ApiResponse.<T>builder().status(ApiStatus.SUCCESS).data(data).message(message).build();
	}

	public static <T> ApiResponse<T> error(String message, T data) {
		return ApiResponse.<T>builder().status(ApiStatus.ERROR).data(data).message(message).build();
	}
}
