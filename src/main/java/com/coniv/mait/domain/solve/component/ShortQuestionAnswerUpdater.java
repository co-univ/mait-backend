package com.coniv.mait.domain.solve.component;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.component.ShortQuestionValidator;
import com.coniv.mait.web.solve.dto.ShortUpdateAnswerPayload;
import com.coniv.mait.web.solve.dto.UpdateAnswerPayload;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShortQuestionAnswerUpdater implements QuestionAnswerUpdater {

	private final ShortAnswerEntityRepository shortAnswerEntityRepository;
	private final ShortQuestionValidator shortQuestionValidator;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.SHORT;
	}

	@Override
	@Transactional
	public void updateAnswer(QuestionEntity question, UpdateAnswerPayload payload) {
		if (!(payload instanceof ShortUpdateAnswerPayload answers)) {
			throw new IllegalArgumentException("단답형 문제에만 ShortQuestionAnswerUpdater를 사용할 수 있습니다.");
		}

		shortQuestionValidator.validateAcceptedAnswerAddition(question.getId(), answers.shortAnswers());

		List<ShortAnswerEntity> shortAnswers = answers.shortAnswers().stream()
			.map(dto -> ShortAnswerEntity.builder()
				.number(dto.getNumber())
				.answer(dto.getAnswer())
				.isMain(false)
				.shortQuestionId(question.getId())
				.build())
			.toList();

		shortAnswerEntityRepository.saveAll(shortAnswers);
	}
}
