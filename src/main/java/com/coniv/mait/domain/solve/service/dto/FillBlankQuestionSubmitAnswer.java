package com.coniv.mait.domain.solve.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FillBlankQuestionSubmitAnswer implements SubmitAnswerDto<FillBlankSubmitAnswer> {

	private List<FillBlankSubmitAnswer> submitAnswers;

	@Override
	public QuestionType getType() {
		return QuestionType.FILL_BLANK;
	}

	@Override
	public List<FillBlankSubmitAnswer> getSubmitAnswers() {
		return submitAnswers;
	}
}
