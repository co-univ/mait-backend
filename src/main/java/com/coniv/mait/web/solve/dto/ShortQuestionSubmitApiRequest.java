package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.solve.service.dto.ShortQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ShortQuestionSubmitApiRequest extends QuestionAnswerSubmitApiRequest {

	@NotNull(message = "주관식 문제의 답변은 필수입니다.")
	@Size(min = 1, message = "주관식 문제는 최소 1글자 이상의 답변이 필요합니다.")
	private List<String> submitAnswers;

	@Override
	public SubmitAnswerDto<String> getSubmitAnswers() {
		return new ShortQuestionSubmitAnswer(submitAnswers);
	}
}
