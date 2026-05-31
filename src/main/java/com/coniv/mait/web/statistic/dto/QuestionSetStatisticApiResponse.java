package com.coniv.mait.web.statistic.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.statistic.service.dto.QuestionSetStatisticDto;
import com.coniv.mait.domain.statistic.service.dto.QuestionSetWinnerDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record QuestionSetStatisticApiResponse(
	@Schema(description = "문제 셋 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long questionSetId,

	@Schema(description = "문제 셋 제목", requiredMode = Schema.RequiredMode.REQUIRED)
	String title,

	@Schema(description = "풀이 모드 (LIVE_TIME: 실시간, STUDY: 학습)", requiredMode = Schema.RequiredMode.REQUIRED)
	QuestionSetSolveMode solveMode,

	@Schema(description = "풀이 날짜", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDateTime solvedAt,

	@Schema(description = "우승자 목록 (실시간 전용, 없으면 빈 배열)", requiredMode = Schema.RequiredMode.REQUIRED)
	List<QuestionSetWinnerDto> winners,

	@Schema(description = "내 정답률 (%, 소수 첫째 자리, 풀지 않은 경우 null)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	Double myCorrectRate,

	@Schema(description = "전체 평균 정답률 (%, 소수 첫째 자리)", requiredMode = Schema.RequiredMode.REQUIRED)
	double averageCorrectRate
) {

	public static QuestionSetStatisticApiResponse from(final QuestionSetStatisticDto dto) {
		List<QuestionSetWinnerDto> winners = dto.getWinners() == null ? List.of() : dto.getWinners();
		return QuestionSetStatisticApiResponse.builder()
			.questionSetId(dto.getQuestionSetId())
			.title(dto.getTitle())
			.solveMode(dto.getSolveMode())
			.solvedAt(dto.getSolvedAt())
			.winners(winners)
			.myCorrectRate(dto.getMyCorrectRate())
			.averageCorrectRate(dto.getAverageCorrectRate())
			.build();
	}
}
