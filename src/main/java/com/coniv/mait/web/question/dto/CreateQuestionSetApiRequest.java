package com.coniv.mait.web.question.dto;

import java.util.Collections;
import java.util.List;

import com.coniv.mait.domain.question.dto.MaterialDto;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.service.dto.QuestionCount;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionSetApiRequest(
	@NotNull(message = "팀 정보는 필수 입니다.")
	Long teamId,
	@Schema(description = "문제 셋 제목")
	String title,
	@Deprecated
	@Schema(description = "문제 셋 제목(deprecated, title로 대체)", deprecated = true)
	String subject,
	@NotNull(message = "문제 셋 생성 유형을 선택해주세요.")
	QuestionSetCreationType creationType,
	@Schema(description = "문제 풀이 방식 (실시간/학습)", enumAsRef = true, examples = {"STUDY", "LIVE_TIME"})
	@NotNull(message = "문제 풀이 방식을 선택해주세요.")
	QuestionSetSolveMode solveMode,
	@Schema(description = "업로드한 해당 문제 셋의 파일 목록")
	List<MaterialDto> materials,
	@Schema(description = "제작 요청할 문제 개수")
	List<@Valid QuestionCount> counts,
	@Schema(description = "문제 난이도, AI 생성인 경우에만 활용")
	String difficulty,
	@Schema(description = "문제 셋에 대한 보충 설명, AI 생성인 경우에만 활용")
	String instruction,
	@Schema(description = "문제 셋에 부착할 카테고리 ID 목록")
	List<Long> categoryIds
) {

	public CreateQuestionSetApiRequest(
		final Long teamId,
		final String title,
		final QuestionSetCreationType creationType,
		final QuestionSetSolveMode solveMode,
		final List<MaterialDto> materials,
		final List<@Valid QuestionCount> counts,
		final String difficulty,
		final String instruction,
		final List<Long> categoryIds
	) {
		this(teamId, title, null, creationType, solveMode, materials, counts, difficulty, instruction,
			categoryIds);
	}

	public CreateQuestionSetApiRequest {
		if (creationType == QuestionSetCreationType.AI_GENERATED && materials == null) {
			materials = Collections.emptyList();
		}
	}

	public QuestionSetDto toQuestionSetDto() {
		return QuestionSetDto.builder()
			.teamId(teamId)
			.title(resolvedTitle())
			.creationType(creationType)
			.solveMode(solveMode)
			.materials(materials)
			.build();
	}

	@AssertTrue(message = "문제 셋 제목을 입력해주세요.")
	private boolean isTitleProvided() {
		return hasText(title) || hasText(subject);
	}

	private String resolvedTitle() {
		if (hasText(title)) {
			return title;
		}
		return subject;
	}

	private static boolean hasText(final String value) {
		return value != null && !value.isBlank();
	}
}
