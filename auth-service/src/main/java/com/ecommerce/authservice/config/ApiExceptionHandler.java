package com.ecommerce.authservice.config;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleBadRequestBody(HttpMessageNotReadableException ex) {
		return Map.of("error", "Invalid request body.");
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
		String message = ex.getMessage();
		if (message == null || message.isBlank()) {
			message = "Request failed.";
		}

		HttpStatus status = ("User not found".equalsIgnoreCase(message) || "Wrong password".equalsIgnoreCase(message))
				? HttpStatus.UNAUTHORIZED
				: HttpStatus.BAD_REQUEST;

		return ResponseEntity.status(status).body(Map.of("error", message));
	}
}
