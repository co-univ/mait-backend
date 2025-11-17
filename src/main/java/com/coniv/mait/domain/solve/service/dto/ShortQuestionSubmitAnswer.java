package com.coniv.mait.domain.solve.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShortQuestionSubmitAnswer implements SubmitAnswerDto<String> {

	@Schema(description = "사용자가 입력한 답변", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<String> submitAnswers;

	@Override
	public QuestionType getType() {
		return QuestionType.SHORT;
	}

	@Override
	public List<String> getSubmitAnswers() {
		return submitAnswers;
	}
}
