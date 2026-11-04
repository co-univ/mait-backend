package com.coniv.mait.web.solve.dto;

import java.util.List;
import java.util.Objects;

import org.springframework.util.StringUtils;

import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record FillBlankUpdateAnswerPayload(
	@NotEmpty List<@NotNull @Valid FillBlankAnswerDto> answers
) implements UpdateAnswerPayload {

	@JsonIgnore
	@AssertTrue(message = "메인 정답은 추가할 수 없습니다. 인정답안만 추가해주세요.")
	public boolean isAcceptedAnswersOnly() {
		return answers == null || answers.stream().filter(Objects::nonNull)
			.noneMatch(FillBlankAnswerDto::isMain);
	}

	@JsonIgnore
	@AssertTrue(message = "인정답안은 비어 있을 수 없습니다.")
	public boolean isAnswerContentValid() {
		return answers == null || answers.stream().filter(Objects::nonNull)
			.allMatch(answer -> StringUtils.hasText(answer.getAnswer()));
	}

	@Override
	public QuestionType getType() {
		return QuestionType.FILL_BLANK;
	}
}

