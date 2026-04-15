package com.coniv.mait.web.question.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "문제 셋 목록 조회 V2 응답")
public record QuestionSetViewApiResponse(
	@Schema(description = "문제 셋 목록 조회 관점")
	QuestionSetView view,
	@Schema(description = "문제 셋 목록 조회 카테고리")
	QuestionSetViewType type,
	@Schema(description = "문제 셋 목록 그룹화 기준")
	QuestionSetGroupBy groupBy,
	@Schema(description = "그룹화된 문제 셋 섹션 목록")
	List<QuestionSetViewSection> sections
) {

	public static QuestionSetViewApiResponse of(final QuestionSetView view, final QuestionSetViewType type,
		final QuestionSetGroupBy groupBy, final List<QuestionSetViewSection> sections) {
		return QuestionSetViewApiResponse.builder()
			.view(view)
			.type(type)
			.groupBy(groupBy)
			.sections(sections)
			.build();
	}
}
