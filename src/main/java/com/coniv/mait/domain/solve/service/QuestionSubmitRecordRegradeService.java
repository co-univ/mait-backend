package com.coniv.mait.domain.solve.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionExceptionCode;
import com.coniv.mait.domain.question.exception.QuestionStatusException;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionSubmitRecordRegradeService {

	private final QuestionReader questionReader;
	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;
	private final AnswerGrader answerGrader;

	@Transactional
	public void regradeSubmitRecords(final Long questionId) {
		QuestionEntity question = questionReader.getQuestion(questionId);
		if (question.getType() != QuestionType.SHORT && question.getType() != QuestionType.FILL_BLANK) {
			log.warn("[재채점 실패] 예상치 못한 타입의 재채점 시도 type={}", question.getType());
			throw new QuestionStatusException(QuestionExceptionCode.UNAVAILABLE_TYPE);
		}

		List<AnswerSubmitRecordEntity> submitRecords = answerSubmitRecordEntityRepository.findAllByQuestionId(
			question.getId());

		for (AnswerSubmitRecordEntity submitRecord : submitRecords) {
			SubmitAnswerDto<?> submitAnswer = SubmitAnswerDto.fromJson(submitRecord.getSubmittedAnswer());
			boolean regradedResult = answerGrader.gradeAnswer(question, submitAnswer);
			submitRecord.updateCorrect(regradedResult);
		}
	}
}

