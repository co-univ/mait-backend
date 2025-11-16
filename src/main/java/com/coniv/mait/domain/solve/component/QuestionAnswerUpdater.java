package com.coniv.mait.domain.solve.component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.web.solve.dto.UpdateAnswerPayload;

public interface QuestionAnswerUpdater {
	QuestionType getQuestionType();

	void updateAnswer(QuestionEntity question, QuestionType type, UpdateAnswerPayload payload);
}
