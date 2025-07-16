package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.QuestionSetCreationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionSetApiRequest(

	// Todo 팀 & 생성한 유저 정보 추가
	// Long teamId,

	// Long userId,
	@NotBlank(message = "교육 주제를 입력해주세요.")
	String subject,

	@NotNull(message = "문제 셋 생성 유형을 선택해주세요.")
	QuestionSetCreationType creationType
) {
}
