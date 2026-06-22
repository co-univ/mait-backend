package com.coniv.mait.web.question.dto;

import java.util.Collections;
import java.util.List;

import com.coniv.mait.domain.question.dto.MaterialDto;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.service.dto.QuestionCount;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionSetApiRequest(
	@NotNull(message = "팀 정보는 필수 입니다.")
	Long teamId,
	@NotBlank(message = "문제 셋 제목을 입력해주세요.")
	String title,
	@NotNull(message = "문제 셋 생성 유형을 선택해주세요.")
	QuestionSetCreationType creationType,
	@Schema(description = "문제 풀이 방식 (실시간/학습)", enumAsRef = true, examples = {"STUDY", "LIVE_TIME"})
	@NotNull(message = "문제 풀이 방식을 선택해주세요.")
	QuestionSetSolveMode solveMode,
	@Schema(description = "문제 셋 공개 단위, 미지정 시 GROUP", enumAsRef = true)
	@NotNull(message = "문제 셋 공개 범위를 입력해주세요.")
	QuestionSetVisibility visibility,
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

	public CreateQuestionSetApiRequest {
		if (creationType == QuestionSetCreationType.AI_GENERATED && materials == null) {
			materials = Collections.emptyList();
		}
	}

	public QuestionSetDto toQuestionSetDto() {
		return QuestionSetDto.builder()
			.teamId(teamId)
			.title(title)
			.creationType(creationType)
			.solveMode(solveMode)
			.visibility(visibility)
			.materials(materials)
			.build();
	}
}
