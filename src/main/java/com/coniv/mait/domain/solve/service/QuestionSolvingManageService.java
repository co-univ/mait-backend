package com.coniv.mait.domain.solve.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionExceptionCode;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.QuestionStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.solve.component.QuestionAnswerUpdater;
import com.coniv.mait.domain.solve.event.QuestionUpdateEvent;
import com.coniv.mait.global.event.MaitEventPublisher;
import com.coniv.mait.web.solve.dto.UpdateAnswerPayload;

@Service
public class QuestionSolvingManageService {
	private final QuestionReader questionReader;

	private final Map<QuestionType, QuestionAnswerUpdater> questionAnswerUpdaters;

	private final MaitEventPublisher maitEventPublisher;

	public QuestionSolvingManageService(List<QuestionAnswerUpdater> updaters, QuestionReader questionReader,
		MaitEventPublisher maitEventPublisher) {
		this.questionAnswerUpdaters = updaters.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionAnswerUpdater::getQuestionType, Function.identity()));
		this.questionReader = questionReader;
		this.maitEventPublisher = maitEventPublisher;
	}

	@Transactional
	public void updateQuestionAnswers(final Long questionSetId, final Long questionId,
		final UpdateAnswerPayload request) {
		QuestionEntity question = questionReader.getQuestion(questionId, questionSetId);

		QuestionSetEntity questionSet = question.getQuestionSet();

		if (!questionSet.isOnLive()) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_LIVE_TIME);
		}

		QuestionType type = request.getType();
		if (type != QuestionType.SHORT && type != QuestionType.FILL_BLANK) {
			throw new QuestionStatusException(QuestionExceptionCode.UNAVAILABLE_TYPE);
		}

		QuestionAnswerUpdater questionAnswerUpdater = getQuestionAnswerUpdater(type);
		questionAnswerUpdater.updateAnswer(question, request);

		maitEventPublisher.publishEvent(QuestionUpdateEvent.builder().questionId(questionId).build());
	}

	public QuestionAnswerUpdater getQuestionAnswerUpdater(QuestionType type) {
		return questionAnswerUpdaters.get(type);
	}
}
