package com.coniv.mait.domain.question.service.component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;

public interface QuestionValidator {

	QuestionType getQuestionType();

	QuestionValidateDto validate(QuestionEntity question);

	static boolean isQuestionValid(final QuestionEntity question) {
		return question.getContent() != null && !question.getContent().isBlank();
	}
}
