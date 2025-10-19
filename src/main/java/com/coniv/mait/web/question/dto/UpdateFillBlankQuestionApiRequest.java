package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateFillBlankQuestionApiRequest extends UpdateQuestionApiRequest {

	@Valid
	@NotNull(message = "빈칸 문제 정답은 필수입니다.")
	private List<FillBlankAnswerDto> fillBlankAnswers;

	@Override
	public QuestionDto toQuestionDto() {
		return FillBlankQuestionDto.builder()
			.id(getId())
			.content(getContent())
			.explanation(getExplanation())
			.imageUrl(getImageUrl())
			.imageId(getImageId())
			.number(getNumber())
			.fillBlankAnswers(fillBlankAnswers)
			.build();
	}
}
