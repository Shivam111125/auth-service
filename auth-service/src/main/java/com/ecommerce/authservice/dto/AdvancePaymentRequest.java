package com.ecommerce.authservice.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class AdvancePaymentRequest {
	private BigDecimal amount;
	private String paymentMode;
	private String note;
	private String month;
}
