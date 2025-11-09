package com.coniv.mait.domain.question.service.component;

import java.util.List;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.dto.QuestionDto;

public interface QuestionFactory<T extends QuestionDto> {

	QuestionType getQuestionType();

	QuestionEntity save(T questionDto, QuestionSetEntity questionSetEntity);

	QuestionDto getQuestion(QuestionEntity question, boolean answerVisible);

	QuestionEntity create(T questionDto, QuestionSetEntity questionSetEntity);

	QuestionEntity createDefaultQuestion(String lexoRank, QuestionSetEntity questionSetEntity);

	void deleteSubEntities(QuestionEntity question);

	void createSubEntities(T questionDto, QuestionEntity question);

	static List<MultipleChoiceEntity> createDefaultSubEntities(MultipleQuestionEntity defaultQuestion) {
		return List.of(
			MultipleChoiceEntity.defaultChoice(1, defaultQuestion),
			MultipleChoiceEntity.defaultChoice(2, defaultQuestion),
			MultipleChoiceEntity.defaultChoice(3, defaultQuestion),
			MultipleChoiceEntity.defaultChoice(4, defaultQuestion)
		);
	}
}
