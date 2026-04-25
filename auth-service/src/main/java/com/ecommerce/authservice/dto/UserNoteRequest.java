package com.ecommerce.authservice.dto;

import lombok.Data;

@Data
public class UserNoteRequest {
	private String note;
	private Boolean starred;
}
