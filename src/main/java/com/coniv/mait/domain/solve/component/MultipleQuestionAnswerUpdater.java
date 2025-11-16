package com.coniv.mait.domain.solve.component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
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
	public void updateAnswer(QuestionEntity question, UpdateAnswerPayload payload) {
		if (!(question instanceof MultipleQuestionEntity multipleQuestion)) {
			throw new IllegalArgumentException("객관식 문제에만 MultipleQuestionAnswerUpdater를 사용할 수 있습니다.");
		}

		MultipleChoiceUpdateAnswerPayload answers = (MultipleChoiceUpdateAnswerPayload)payload;
		Set<Long> correctChoiceIds = answers.correctChoiceIds();

		List<MultipleChoiceEntity> choices = multipleChoiceEntityRepository.findAllByQuestionId(question.getId());
		Set<Long> choiceIds = choices.stream()
			.map(MultipleChoiceEntity::getId)
			.collect(Collectors.toSet());

		if (!choiceIds.containsAll(correctChoiceIds)) {
			throw new IllegalArgumentException("존재하지 않는 보기 ID가 포함되어 있습니다.");
		}

		choices.forEach(choice -> choice.updateIsCorrect(correctChoiceIds.contains(choice.getId())));
		multipleQuestion.updateAnswerCount(correctChoiceIds.size());
	}
}
