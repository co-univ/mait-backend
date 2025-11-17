package com.coniv.mait.domain.solve.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.enums.QuestionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MultipleQuestionSubmitAnswer implements SubmitAnswerDto<Long> {

	@Schema(description = "선택한 번호 목록")
	@NotEmpty(message = "선택한 번호를 입력해주세요.")
	private List<Long> submitAnswers;

	@Override
	public QuestionType getType() {
		return QuestionType.MULTIPLE;
	}

	@Override
	public List<Long> getSubmitAnswers() {
		return this.submitAnswers;
	}
}
