package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuestionSetCategoryDto {

	private final Long id;
	private final Long teamId;
	private final String name;

	public static QuestionSetCategoryDto from(final QuestionSetCategoryEntity entity) {
		return QuestionSetCategoryDto.builder()
			.id(entity.getId())
			.teamId(entity.getTeamId())
			.name(entity.getName())
			.build();
	}
}
