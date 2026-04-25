package com.ecommerce.authservice.dto;

import java.util.List;

import lombok.Data;

@Data
public class AdvancePaymentExportRequest {
	private List<Long> userIds;
	private String month;
}
