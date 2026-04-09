package com.coniv.mait.domain.question.service.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.coniv.mait.domain.question.dto.MaterialDto;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionSetDto {
	private Long id;
	private String subject;
	private String title;
	private QuestionSetCreationType creationType;
	private QuestionSetVisibility visibility;
	private DeliveryMode deliveryMode;
	private QuestionSetSolveMode solveMode;
	private QuestionSetStatus ongoingStatus;
	private Long teamId;
	private Long questionCount;
	private String difficulty;
	private List<MaterialDto> materials;
	private LocalDateTime updatedAt;

	public static QuestionSetDto from(final QuestionSetEntity questionSetEntity) {
		return QuestionSetDto.builder()
			.id(questionSetEntity.getId())
			.subject(questionSetEntity.getSubject())
			.title(questionSetEntity.getTitle())
			.creationType(questionSetEntity.getCreationType())
			.visibility(questionSetEntity.getVisibility())
			.deliveryMode(questionSetEntity.getDisplayMode())
			.solveMode(questionSetEntity.getSolveMode())
			.ongoingStatus(questionSetEntity.getStatus())
			.teamId(questionSetEntity.getTeamId())
			.difficulty(questionSetEntity.getDifficulty())
			.updatedAt(questionSetEntity.getModifiedAt())
			.build();
	}

	public static QuestionSetDto of(QuestionSetEntity questionSetEntity, long questionCount) {
		return QuestionSetDto.builder()
			.id(questionSetEntity.getId())
			.subject(questionSetEntity.getSubject())
			.title(questionSetEntity.getTitle())
			.creationType(questionSetEntity.getCreationType())
			.visibility(questionSetEntity.getVisibility())
			.deliveryMode(questionSetEntity.getDisplayMode())
			.solveMode(questionSetEntity.getSolveMode())
			.teamId(questionSetEntity.getTeamId())
			.difficulty(questionSetEntity.getDifficulty())
			.ongoingStatus(questionSetEntity.getStatus())
			.updatedAt(questionSetEntity.getModifiedAt())
			.questionCount(questionCount)
			.build();
	}
}
