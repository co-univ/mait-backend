package com.coniv.mait.domain.solve.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.exception.QuestionSolveExceptionCode;
import com.coniv.mait.domain.solve.exception.QuestionSolvingException;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.service.component.AnswerGrader;
import com.coniv.mait.domain.solve.service.component.QuestionSetParticipantManager;
import com.coniv.mait.domain.solve.service.component.ScorerGenerator;
import com.coniv.mait.domain.solve.service.component.ScorerProcessor;
import com.coniv.mait.domain.solve.service.component.SubmitOrderGenerator;
import com.coniv.mait.domain.solve.service.dto.AnswerSubmitDto;
import com.coniv.mait.domain.solve.service.dto.AnswerSubmitRecordDto;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionAnswerSubmitService {

	private final UserEntityRepository userEntityRepository;

	private final QuestionReader questionReader;

	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	private final AnswerGrader answerGrader;

	private final SubmitOrderGenerator submitOrderGenerator;

	private final ScorerProcessor scorerProcessor;

	private final ScorerGenerator scorerGenerator;

	private final TeamRoleValidator teamRoleValidator;

	private final QuestionSetParticipantManager questionSetParticipantManager;

	private final ObjectMapper objectMapper;

	@Transactional
	public AnswerSubmitDto submitAnswer(final Long questionSetId, final Long questionId, final Long userId,
		final SubmitAnswerDto<?> submitAnswer) throws JsonProcessingException {
		final Long submitOrder = submitOrderGenerator.generateSubmitOrder(questionId);
		final UserEntity user = userEntityRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

		final QuestionEntity question = questionReader.getQuestion(questionId, questionSetId);

		if (!question.canSolve()) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.CANNOT_SOLVE);
		}

		final QuestionSetEntity questionSet = question.getQuestionSet();
		final Long teamId = questionSet.getTeamId();

		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(teamId, userId);

		if (questionSet.getVisibility() == QuestionSetVisibility.PRIVATE) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.NEED_OPEN);
		}

		if (answerSubmitRecordEntityRepository.existsByUserIdAndQuestionIdAndIsCorrectTrue(user.getId(), questionId)) {
			throw new QuestionSolvingException(QuestionSolveExceptionCode.ALREADY);
		}

		final boolean isCorrect = answerGrader.gradeAnswer(question, submitAnswer);

		if (isCorrect && user.getId().equals(scorerProcessor.getScorer(questionId, user.getId(), submitOrder))) {
			scorerGenerator.updateScorer(questionId, user.getId(), submitOrder);
		}

		AnswerSubmitRecordEntity submitRecord = AnswerSubmitRecordEntity.builder()
			.userId(user.getId())
			.questionId(question.getId())
			.isCorrect(isCorrect)
			.submitOrder(submitOrder)
			.submittedAnswer(objectMapper.writeValueAsString(submitAnswer))
			.build();

		answerSubmitRecordEntityRepository.save(submitRecord);

		return AnswerSubmitDto.from(submitRecord);
	}

	public List<AnswerSubmitRecordDto> getSubmitRecords(final Long questionSetId, final Long questionId) {
		final QuestionEntity question = questionReader.getQuestion(questionId, questionSetId);

		List<AnswerSubmitRecordEntity> records = answerSubmitRecordEntityRepository.findAllByQuestionId(questionId);
		List<Long> userIds = records.stream().map(AnswerSubmitRecordEntity::getUserId).toList();

		Map<Long, UserEntity> userById = userEntityRepository.findAllById(userIds).stream()
			.collect(Collectors.toUnmodifiableMap(UserEntity::getId, user -> user));

		return records.stream()
			.sorted(Comparator.comparing(AnswerSubmitRecordEntity::getSubmitOrder))
			.map(record -> AnswerSubmitRecordDto.of(record, userById.get(record.getUserId())))
			.toList();
	}
}
