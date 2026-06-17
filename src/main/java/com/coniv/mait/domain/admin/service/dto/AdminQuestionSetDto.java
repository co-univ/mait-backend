package com.coniv.mait.domain.admin.service.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminQuestionSetDto {

	private final Long id;
	private final String title;
	private final String subject;
	private final QuestionSetStatus status;
	private final QuestionSetSolveMode solveMode;
	private final Long teamId;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public static AdminQuestionSetDto from(final QuestionSetEntity questionSet) {
		return AdminQuestionSetDto.builder()
			.id(questionSet.getId())
			.title(questionSet.getTitle())
			.subject(questionSet.getSubject())
			.status(questionSet.getStatus())
			.solveMode(questionSet.getSolveMode())
			.teamId(questionSet.getTeamId())
			.createdAt(questionSet.getCreatedAt())
			.updatedAt(questionSet.getModifiedAt())
			.build();
	}
}
