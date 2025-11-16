package com.coniv.mait.domain.solve.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.solve.component.QuestionAnswerUpdater;
import com.coniv.mait.web.solve.dto.UpdateAnswerPayload;

@Service
public class QuestionSolvingManageService {
	private final QuestionReader questionReader;

	private final Map<QuestionType, QuestionAnswerUpdater> questionAnswerUpdaters;

	public QuestionSolvingManageService(List<QuestionAnswerUpdater> updaters, QuestionReader questionReader) {
		this.questionAnswerUpdaters = updaters.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionAnswerUpdater::getQuestionType, Function.identity()));
		this.questionReader = questionReader;
	}

	/**
	 * 특정 문제에 대해 재채점을 수행한다.
	 */
	public void updateQuestionAnswers(final Long questionSetId, final Long questionId,
		final QuestionType type, final UpdateAnswerPayload request) {
		QuestionEntity question = questionReader.getQuestion(questionId, questionSetId);
		// Todo 문제 셋이 풀이 중인지 확인
		// QuestionSetEntity questionSet = question.getQuestionSet();
		//
		// if (!questionSet.isOnLive()) {
		// 	throw new RuntimeException("문제 세트가 라이브 상태가 아닙니다.");
		// }
		QuestionAnswerUpdater questionAnswerUpdater = getQuestionAnswerUpdater(type);

		questionAnswerUpdater.updateAnswer(question, request);
	}

	public QuestionAnswerUpdater getQuestionAnswerUpdater(QuestionType type) {
		return questionAnswerUpdaters.get(type);
	}
}
