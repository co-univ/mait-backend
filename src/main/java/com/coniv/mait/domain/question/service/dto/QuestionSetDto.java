package com.coniv.mait.domain.question.service.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
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
	private Long teamId;
	private Long questionCount;
	private String levelDescription;
	private LocalDateTime updatedAt;

	public static QuestionSetDto from(final QuestionSetEntity questionSetEntity) {
		return QuestionSetDto.builder()
			.id(questionSetEntity.getId())
			.subject(questionSetEntity.getSubject())
			.title(questionSetEntity.getTitle())
			.creationType(questionSetEntity.getCreationType())
			.visibility(questionSetEntity.getVisibility())
			.deliveryMode(questionSetEntity.getDeliveryMode())
			.teamId(questionSetEntity.getTeamId())
			.levelDescription(questionSetEntity.getLevelDescription())
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
			.deliveryMode(questionSetEntity.getDeliveryMode())
			.teamId(questionSetEntity.getTeamId())
			.updatedAt(questionSetEntity.getModifiedAt())
			.questionCount(questionCount)
			.build();
	}
}
