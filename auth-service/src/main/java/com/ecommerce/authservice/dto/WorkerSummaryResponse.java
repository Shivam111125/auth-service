package com.ecommerce.authservice.dto;

import java.math.BigDecimal;

public record WorkerSummaryResponse(
		Long id,
		String name,
		String email,
		String role,
		BigDecimal advanceAmount,
		BigDecimal lifetimeAdvanceAmount,
		String starredNotePreview,
		boolean hasStarredNote) {
}
