package com.coniv.mait.domain.question.service.dto;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionValidationResult;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionValidateDto {

	private Long questionId;

	private Long number;

	private boolean valid;

	private QuestionValidationResult reason;

	public static QuestionValidateDto invalid(QuestionEntity question, QuestionValidationResult reason) {
		return QuestionValidateDto.builder()
			.questionId(question.getId())
			.number(question.getNumber())
			.valid(false)
			.reason(reason)
			.build();
	}

	public static QuestionValidateDto valid(QuestionEntity question) {
		return QuestionValidateDto.builder()
			.questionId(question.getId())
			.number(question.getNumber())
			.valid(true)
			.build();
	}
}
