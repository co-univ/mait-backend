package com.coniv.mait.domain.solve.service;

import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionSolvingManageService {

	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	private final QuestionReader questionReader;

	/**
	 * 특정 문제에 대해 재채점을 수행한다.
	 */
	public void updateQuestionAnswers(final Long questionSetId, final Long questionId) {
		QuestionEntity question = questionReader.getQuestion(questionId, questionSetId);
		// Todo 문제 셋이 풀이 중인지 확인
		// QuestionSetEntity questionSet = question.getQuestionSet();
		//
		// if (!questionSet.isOnLive()) {
		// 	throw new RuntimeException("문제 세트가 라이브 상태가 아닙니다.");
		// }
		//

	}
}
