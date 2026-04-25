package com.ecommerce.authservice.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class WorkerRequest {
	private String name;
	private BigDecimal advanceAmount;
	private String paymentMode;
	private String note;
	private String month;
}
