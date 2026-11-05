package com.coniv.mait.domain.solve.component;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.service.component.FillBlankQuestionValidator;
import com.coniv.mait.web.solve.dto.FillBlankUpdateAnswerPayload;
import com.coniv.mait.web.solve.dto.UpdateAnswerPayload;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FillBlankQuestionAnswerUpdater implements QuestionAnswerUpdater {

	private final FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;
	private final FillBlankQuestionValidator fillBlankQuestionValidator;

	@Override
	public QuestionType getQuestionType() {
		return QuestionType.FILL_BLANK;
	}

	@Override
	@Transactional
	public void updateAnswer(QuestionEntity question, UpdateAnswerPayload payload) {
		if (!(payload instanceof FillBlankUpdateAnswerPayload fillBlankPayload)) {
			throw new IllegalArgumentException("빈칸 채우기 문제에만 FillBlankQuestionAnswerUpdater를 사용할 수 있습니다.");
		}

		fillBlankQuestionValidator.validateAcceptedAnswerAddition(question.getId(), fillBlankPayload.answers());

		List<FillBlankAnswerEntity> answers = fillBlankPayload.answers().stream()
			.map(dto -> FillBlankAnswerEntity.builder()
				.fillBlankQuestionId(question.getId())
				.number(dto.getNumber())
				.answer(dto.getAnswer())
				.isMain(false)
				.build())
			.toList();
		fillBlankAnswerEntityRepository.saveAll(answers);
	}
}
