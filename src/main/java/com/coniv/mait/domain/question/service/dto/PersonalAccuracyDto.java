package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.global.util.MathUtil;

import lombok.Builder;

@Builder
public record PersonalAccuracyDto(
	long totalSolvedCount,
	long correctCount,
	double accuracyRate
) {
	public static PersonalAccuracyDto of(long totalSolvedCount, long correctCount) {
		return new PersonalAccuracyDto(totalSolvedCount, correctCount,
			MathUtil.calculateRateRoundedToFirstDecimal(correctCount, totalSolvedCount));
	}
}
