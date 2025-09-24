package com.coniv.mait.domain.question.service.component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.QuestionDto;

public interface QuestionFactory<T extends QuestionDto> {

	QuestionType getQuestionType();

	void save(T questionDto, QuestionSetEntity questionSetEntity);

	QuestionDto getQuestion(QuestionEntity question, boolean answerVisible);

	void deleteSubEntities(QuestionEntity question);

	void createSubEntities(T questionDto, QuestionEntity question);
}
