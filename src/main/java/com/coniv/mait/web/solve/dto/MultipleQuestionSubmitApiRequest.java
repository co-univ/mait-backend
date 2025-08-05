package com.coniv.mait.web.solve.dto;

import java.util.List;

import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class MultipleQuestionSubmitApiRequest extends QuestionAnswerSubmitApiRequest {

	@NotNull(message = "객관식 문제의 선택지는 필수입니다.")
	@Size(min = 1, message = "객관식 문제는 최소 1개의 선택지를 선택해야 합니다.")
	private List<Long> submitAnswers;

	@Override
	public SubmitAnswerDto<Long> getSubmitAnswers() {
		return new MultipleQuestionSubmitAnswer(submitAnswers);
	}
}
