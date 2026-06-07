package com.coniv.mait.domain.statistic.service.dto;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryCorrectRateDto {

	private Long categoryId;
	private String categoryName;
	private int questionSetCount;
	private Double myCorrectRate;
	private double averageCorrectRate;

	public static CategoryCorrectRateDto of(final QuestionSetCategoryEntity category, final int questionSetCount,
		final Double myCorrectRate, final double averageCorrectRate) {
		return CategoryCorrectRateDto.builder()
			.categoryId(category.getId())
			.categoryName(category.getName())
			.questionSetCount(questionSetCount)
			.myCorrectRate(myCorrectRate)
			.averageCorrectRate(averageCorrectRate)
			.build();
	}
}
