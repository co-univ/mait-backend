package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record UpdateQuestionSetApiRequest(
	@Schema(description = "문제 셋 제목")
	String title,
	@Deprecated
	@Schema(description = "문제 셋 제목(deprecated, title로 대체)", deprecated = true)
	String subject,

	@Schema(description = "문제 풀이 방식", enumAsRef = true, examples = {"STUDY", "LIVE_TIME"})
	@NotNull(message = "문제 풀이 방식을 입력해주세요")
	QuestionSetSolveMode solveMode,

	@Schema(description = "문제 셋 난이도 설명")
	String difficulty,

	@Schema(description = "문제 셋에 매핑할 카테고리 ID 목록. null 또는 빈 목록이면 기존 매핑을 모두 제거한다.")
	List<Long> categoryIds
) {
	public UpdateQuestionSetApiRequest(
		final String title,
		final QuestionSetSolveMode solveMode,
		final String difficulty,
		final List<Long> categoryIds
	) {
		this(title, null, solveMode, difficulty, categoryIds);
	}

	public UpdateQuestionSetApiRequest {
		if (categoryIds == null) {
			categoryIds = List.of();
		}
	}

	@AssertTrue(message = "제목을 입력해주세요")
	private boolean isTitleProvided() {
		return hasText(title) || hasText(subject);
	}

	@AssertTrue(message = "문제 풀이 방식은 STUDY 또는 LIVE_TIME만 가능합니다")
	private boolean isSupportedMode() {
		return solveMode == QuestionSetSolveMode.STUDY || solveMode == QuestionSetSolveMode.LIVE_TIME;
	}

	public String resolvedTitle() {
		if (hasText(title)) {
			return title;
		}
		return subject;
	}

	private static boolean hasText(final String value) {
		return value != null && !value.isBlank();
	}
}
