package com.coniv.mait.domain.solve.service.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolvingSessionDto {
	private Long id;
	private Long userId;
	private Long questionSetId;
	private SolvingStatus status;
	private DeliveryMode mode;
	private LocalDateTime startedAt;
	private LocalDateTime submittedAt;

	public static SolvingSessionDto from(SolvingSessionEntity entity) {
		return SolvingSessionDto.builder()
			.id(entity.getId())
			.userId(entity.getUser().getId())
			.questionSetId(entity.getQuestionSet().getId())
			.status(entity.getStatus())
			.mode(entity.getMode())
			.startedAt(entity.getStartedAt())
			.submittedAt(entity.getSubmittedAt())
			.build();
	}
}
