package com.coniv.mait.web.statistic.dto;

import com.coniv.mait.domain.statistic.service.dto.CategoryCorrectRateDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record CategoryCorrectRateApiResponse(
	@Schema(description = "카테고리 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long categoryId,

	@Schema(description = "카테고리 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	String categoryName,

	@Schema(description = "정답률 계산 대상 문제 셋 수 (종료된 문제 셋 기준)", requiredMode = Schema.RequiredMode.REQUIRED)
	int questionSetCount,

	@Schema(description = "내 정답률 (%, 소수 첫째 자리, 풀지 않은 경우 null)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Double myCorrectRate,

	@Schema(description = "전체 평균 정답률 (%, 소수 첫째 자리)", requiredMode = Schema.RequiredMode.REQUIRED)
	double averageCorrectRate
) {

	public static CategoryCorrectRateApiResponse from(final CategoryCorrectRateDto dto) {
		return CategoryCorrectRateApiResponse.builder()
			.categoryId(dto.getCategoryId())
			.categoryName(dto.getCategoryName())
			.questionSetCount(dto.getQuestionSetCount())
			.myCorrectRate(dto.getMyCorrectRate())
			.averageCorrectRate(dto.getAverageCorrectRate())
			.build();
	}
}
