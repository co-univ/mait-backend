package com.coniv.mait.domain.solve.component;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.web.solve.dto.MultipleChoiceUpdateAnswerPayload;
import com.coniv.mait.web.solve.dto.UpdateAnswerPayload;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MultipleQuestionAnswerUpdater implements QuestionAnswerUpdater {

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.MULTIPLE;
	}

	@Override
	@Transactional
	public void updateAnswer(QuestionEntity question, QuestionType type, UpdateAnswerPayload payload) {
		MultipleChoiceUpdateAnswerPayload answers = (MultipleChoiceUpdateAnswerPayload)payload;
		Set<Long> correctChoiceIds = answers.correctChoiceIds();

		List<MultipleChoiceEntity> choices = multipleChoiceEntityRepository.findAllByQuestionId(
			question.getId());
		choices.forEach(
			choice -> choice.updateIsCorrect(correctChoiceIds.contains(choice.getId()))
		);
	}
}
