package com.coniv.mait.domain.solve.service.dto;

import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnswerSubmitRecordDto {
	private Long id;
	private Long userId;
	private String userName;
	private Long questionId;
	private boolean isCorrect;
	private Long submitOrder;

	public static AnswerSubmitRecordDto of(AnswerSubmitRecordEntity record, UserEntity userEntity) {
		return AnswerSubmitRecordDto.builder()
			.id(record.getId())
			.userId(userEntity.getId())
			.userName(userEntity.getName())
			.questionId(record.getQuestionId())
			.isCorrect(record.isCorrect())
			.submitOrder(record.getSubmitOrder())
			.build();
	}
}
