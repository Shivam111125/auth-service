package com.ecommerce.authservice.dto;

import java.time.LocalDateTime;

public record StarredNotePreviewResponse(
		Long id,
		Long userId,
		String note,
		LocalDateTime createdAt) {
}
