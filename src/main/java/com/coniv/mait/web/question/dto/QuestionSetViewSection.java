package com.coniv.mait.web.question.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "문제 셋 목록 조회 섹션")
public record QuestionSetViewSection(
	@Schema(description = "섹션 키")
	String key,
	@Schema(description = "섹션 이름")
	String title,
	@Schema(description = "문제 셋 목록")
	List<QuestionSetViewItem> items
) {

	public static QuestionSetViewSection of(final String key, final String title,
		final List<QuestionSetViewItem> items) {
		return QuestionSetViewSection.builder()
			.key(key)
			.title(title)
			.items(items)
			.build();
	}
}
