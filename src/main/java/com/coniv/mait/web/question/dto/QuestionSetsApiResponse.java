package com.coniv.mait.web.question.dto;

import com.coniv.mait.domain.question.enums.DeliveryMode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "문제 셋 목록 응답")
public record QuestionSetsApiResponse(
	@Schema(description = "전달 모드 (MAKING: 제작 중, LIVE_TIME: 실시간, REVIEW: 복습)", example = "MAKING")
	DeliveryMode mode,
	
	@Schema(description = "문제 셋 컨테이너 (모드에 따라 List 또는 Map 구조)", 
		oneOf = {QuestionSetList.class, QuestionSetGroup.class})
	QuestionSetContainer content
) {

	public static QuestionSetsApiResponse of(DeliveryMode mode, QuestionSetContainer questionSets) {
		return QuestionSetsApiResponse.builder()
			.mode(mode)
			.content(questionSets)
			.build();
	}
}
