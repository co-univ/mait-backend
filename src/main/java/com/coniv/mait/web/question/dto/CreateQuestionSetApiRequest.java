package com.coniv.mait.web.question.dto;

import java.util.Collections;
import java.util.List;

import com.coniv.mait.domain.question.dto.MaterialDto;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.service.dto.QuestionCount;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionSetApiRequest(
	@NotNull(message = "팀 정보는 필수 입니다.")
	Long teamId,
	@NotBlank(message = "교육 주제를 입력해주세요.")
	String subject,
	@NotNull(message = "문제 셋 생성 유형을 선택해주세요.")
	QuestionSetCreationType creationType,
	@Schema(description = "업로드한 해당 문제 셋의 파일 목록")
	List<MaterialDto> materials,
	@Schema(description = "제작 요청할 문제 개수")
	List<@Valid QuestionCount> counts,
	@Schema(description = "문제 난이도, AI 생성인 경우에만 활용")
	String difficulty,
	@Schema(description = "문제 셋에 대한 보충 설명, AI 생성인 경우에만 활용")
	String instruction
) {

	public CreateQuestionSetApiRequest {
		if (creationType == QuestionSetCreationType.AI_GENERATED && materials == null) {
			materials = Collections.emptyList();
		}
	}

	public QuestionSetDto toQuestionSetDto() {
		return QuestionSetDto.builder()
			.teamId(teamId)
			.subject(subject)
			.creationType(creationType)
			.materials(materials)
			.build();
	}
}
