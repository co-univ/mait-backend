package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.solve.service.dto.OrderingQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderingQuestionSubmitApiRequest extends QuestionAnswerSubmitApiRequest {

	@NotNull(message = "순서 문제의 답변은 필수입니다.")
	private List<Long> submitAnswers;

	@Override
	public SubmitAnswerDto<Long> getSubmitAnswers() {
		return new OrderingQuestionSubmitAnswer(submitAnswers);
	}
}
