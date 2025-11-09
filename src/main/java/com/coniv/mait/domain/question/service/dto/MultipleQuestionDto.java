package com.coniv.mait.domain.question.service.dto;

import java.util.List;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;

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
public class MultipleQuestionDto extends QuestionDto {

	@NotNull
	@Size(min = 2, max = 8, message = "객관식 선지의 개수는 2 ~ 8개여야 합니다.")
	private List<MultipleChoiceDto> choices;

	@Override
	public QuestionDto toQuestionDto() {
		return this;
	}

	@Override
	public QuestionType getType() {
		return QuestionType.MULTIPLE;
	}

	public static MultipleQuestionDto of(MultipleQuestionEntity multipleQuestion, List<MultipleChoiceEntity> choices,
		final boolean answerVisible) {
		List<MultipleChoiceDto> choiceDtos = choices.stream()
			.map(choice -> MultipleChoiceDto.of(choice, answerVisible))
			.toList();

		return MultipleQuestionDto.builder()
			.id(multipleQuestion.getId())
			.content(multipleQuestion.getContent())
			.explanation(multipleQuestion.getExplanation())
			.number(multipleQuestion.getNumber())
			.imageUrl(multipleQuestion.getImageUrl())
			.imageId(multipleQuestion.getImageId())
			.questionStatus(multipleQuestion.getQuestionStatus())
			.choices(choiceDtos)
			.build();
	}
}
