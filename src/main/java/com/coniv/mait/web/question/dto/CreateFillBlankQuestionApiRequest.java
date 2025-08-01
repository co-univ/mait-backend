package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateFillBlankQuestionApiRequest extends CreateQuestionApiRequest {

	@Valid
	@NotNull(message = "빈칸 문제 정답은 필수입니다.")
	private List<FillBlankAnswerDto> fillBlankAnswers;

	@Override
	public QuestionDto toQuestionDto() {
		return FillBlankQuestionDto.builder()
			.content(getContent())
			.explanation(getExplanation())
			.number(getNumber())
			.fillBlankAnswers(getFillBlankAnswers())
			.build();
	}
}
