package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.solve.service.dto.FillBlankQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.FillBlankSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FillBlankQuestionSubmitApiRequest extends QuestionAnswerSubmitApiRequest {

	@NotNull(message = "빈칸 문제의 답변은 필수입니다.")
	@Valid
	private List<FillBlankSubmitAnswer> submitAnswers;

	@Override
	public SubmitAnswerDto<FillBlankSubmitAnswer> getSubmitAnswers() {
		return new FillBlankQuestionSubmitAnswer(submitAnswers);
	}
}
