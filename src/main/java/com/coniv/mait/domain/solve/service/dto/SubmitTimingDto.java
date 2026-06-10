package com.coniv.mait.domain.solve.service.dto;

public record SubmitTimingDto(
	long submitOrder,
	long timeGapMillis
) {

	private static final String DELIMITER = ":";

	public static SubmitTimingDto from(final String raw) {
		String[] parts = raw.split(DELIMITER);
		return new SubmitTimingDto(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
	}
}
