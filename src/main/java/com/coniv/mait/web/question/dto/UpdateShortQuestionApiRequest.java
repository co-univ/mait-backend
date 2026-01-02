package com.coniv.mait.web.question.dto;

import java.util.List;

import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateShortQuestionApiRequest extends UpdateQuestionApiRequest {

	@Valid
	@NotEmpty(message = "정답은 최소 1개 이상이어야 합니다.")
	private List<ShortAnswerDto> shortAnswers;

	@JsonIgnore
	@AssertTrue(message = "정답 그룹(number)마다 main 정답이 1개씩 있어야 합니다.")
	public boolean isMainAnswerCountMatchesNumberCount() {
		if (shortAnswers == null || shortAnswers.isEmpty()) {
			return true;
		}

		if (shortAnswers.stream().anyMatch(a -> a == null || a.getNumber() == null)) {
			return true;
		}

		long mainAnswerCount = shortAnswers.stream().filter(ShortAnswerDto::isMain).count();
		long numberCount = shortAnswers.stream().map(ShortAnswerDto::getNumber).distinct().count();
		return mainAnswerCount == numberCount;
	}

	@Override
	public QuestionDto toQuestionDto() {
		return ShortQuestionDto.builder()
				.id(getId())
				.content(getContent())
				.explanation(getExplanation())
				.imageUrl(getImageUrl())
				.imageId(getImageId())
				.number(getNumber())
				.answers(shortAnswers)
				.build();
	}
}
