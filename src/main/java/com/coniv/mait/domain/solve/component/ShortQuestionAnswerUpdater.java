package com.coniv.mait.domain.solve.component;

import java.util.List;

import org.springframework.stereotype.Component;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.web.solve.dto.ShortUpdateAnswerPayload;
import com.coniv.mait.web.solve.dto.UpdateAnswerPayload;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ShortQuestionAnswerUpdater implements QuestionAnswerUpdater {

	private final ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.SHORT;
	}

	@Override
	public void updateAnswer(QuestionEntity question, UpdateAnswerPayload payload) {
		if (!(payload instanceof ShortUpdateAnswerPayload answers)) {
			throw new IllegalArgumentException("단답형 문제에만 ShortQuestionAnswerUpdater를 사용할 수 있습니다.");
		}

		// Todo: 인정답안이 아닌 경우 및 존재에 대한 검증 필요

		List<ShortAnswerEntity> shortAnswers = answers.shortAnswers().stream()
			.map(dto -> ShortAnswerEntity.builder()
				.number(dto.getNumber())
				.answer(dto.getAnswer())
				.isMain(dto.isMain())
				.build())
			.toList();

		shortAnswerEntityRepository.saveAll(shortAnswers);
	}
}
