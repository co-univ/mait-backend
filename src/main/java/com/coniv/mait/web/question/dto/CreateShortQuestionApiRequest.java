package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateShortQuestionApiRequest extends CreateQuestionApiRequest {

	@Valid
	@NotEmpty(message = "정답은 최소 1개 이상이어야 합니다.")
	private List<ShortAnswerDto> shortAnswers;

	@Override
	public QuestionDto toQuestionDto() {
		return ShortQuestionDto.builder()
			.content(getContent())
			.explanation(getExplanation())
			.number(getNumber())
			.shortAnswers(getShortAnswers())
			.build();
	}
}
