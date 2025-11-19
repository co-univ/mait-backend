package com.coniv.mait.domain.solve.service.dto;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.user.entity.UserEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionScorerDto {

	private Long id;

	private Long questionId;

	private Long questionNumber;

	private Long userId;

	private String userName;

	private Long submitOrder;

	public static QuestionScorerDto of(QuestionScorerEntity entity, final UserEntity user) {
		return QuestionScorerDto.builder()
			.id(entity.getId())
			.questionId(entity.getQuestionId())
			.userName(user.getName())
			.userId(user.getId())
			.submitOrder(entity.getSubmitOrder())
			.build();
	}

	public static QuestionScorerDto of(final QuestionScorerEntity entity, final QuestionEntity question,
		final UserEntity user) {
		return QuestionScorerDto.builder()
			.id(entity.getId())
			.questionId(question.getId())
			.questionNumber(question.getNumber())
			.userName(user.getName())
			.userId(user.getId())
			.submitOrder(entity.getSubmitOrder())
			.build();
	}
}
