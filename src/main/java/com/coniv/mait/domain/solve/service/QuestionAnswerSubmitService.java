package com.coniv.mait.domain.solve.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.auth.repository.UserEntityRepository;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.dto.AnswerSubmitDto;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionAnswerSubmitService {

	private final UserEntityRepository userEntityRepository;

	private final QuestionEntityRepository questionEntityRepository;

	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	private final AnswerGrader answerGrader;

	private final ObjectMapper objectMapper;

	@Transactional
	public AnswerSubmitDto submitAnswer(final Long questionSetId, final Long questionId, final Long userId,
		final SubmitAnswerDto submitAnswer) {
		final UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

		final QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("문제를 찾을 수 없습니다."));

		// Todo: 문제 상태 검증 로직 추가
		// Todo: 유저가 해당 QuestionSet에 접근할 권한이 있는지 확인

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("문제 세트와 문제 ID가 일치하지 않습니다.");
		}

		final boolean isCorrect = answerGrader.gradeAnswer(question, submitAnswer);

		AnswerSubmitRecordEntity submitRecord = AnswerSubmitRecordEntity.builder()
			.userId(user.getId())
			.questionId(question.getId())
			.isCorrect(isCorrect)
			.submittedAnswer(objectMapper.convertValue(submitAnswer, String.class))
			.build();

		answerSubmitRecordEntityRepository.save(submitRecord);

		return AnswerSubmitDto.from(submitRecord);
	}
}
