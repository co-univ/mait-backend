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
	private String userNickname;
	private Long questionId;
	private boolean isCorrect;
	private Long submitOrder;
	private SubmitAnswerDto<?> submittedAnswer;

	public static AnswerSubmitRecordDto of(AnswerSubmitRecordEntity record, UserEntity userEntity) {
		return AnswerSubmitRecordDto.builder()
			.id(record.getId())
			.userId(userEntity.getId())
			.userName(userEntity.getName())
			.userNickname(userEntity.getNickname())
			.questionId(record.getQuestionId())
			.isCorrect(record.isCorrect())
			.submitOrder(record.getSubmitOrder())
			.submittedAnswer(SubmitAnswerDto.fromJson(record.getSubmittedAnswer()))
			.build();
	}
}
