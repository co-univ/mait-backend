package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.global.util.MathUtil;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PersonalAccuracyDto {

	private final long totalSolvedCount;
	private final long correctCount;
	private final double accuracyRate;

	public static PersonalAccuracyDto of(long totalSolvedCount, long correctCount) {
		return new PersonalAccuracyDto(totalSolvedCount, correctCount,
			MathUtil.calculateRateRoundedToFirstDecimal(correctCount, totalSolvedCount));
	}
}
