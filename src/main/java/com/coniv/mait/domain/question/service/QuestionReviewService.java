package com.coniv.mait.domain.question.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.LastViewedQuestionRedisRepository;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.question.service.component.ReviewAnswerGrader;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.service.dto.ReviewAnswerCheckResult;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;

import jakarta.persistence.EntityNotFoundException;

@Service
public class QuestionReviewService {

	private final TeamRoleValidator teamRoleValidator;
	private final QuestionReader questionReader;

	private final ReviewAnswerGrader reviewAnswerGrader;

	private final LastViewedQuestionRedisRepository lastViewedQuestionRedisRepository;
	private final QuestionSetEntityRepository questionSetEntityRepository;
	private final QuestionSetReader questionSetReader;

	private final Map<QuestionType, QuestionFactory<?>> questionFactories;

	@Autowired
	public QuestionReviewService(QuestionSetEntityRepository questionSetEntityRepository,
		TeamRoleValidator teamRoleValidator,
		LastViewedQuestionRedisRepository lastViewedQuestionRedisRepository,
		ReviewAnswerGrader reviewAnswerGrader,
		QuestionReader questionReader,
		QuestionSetReader questionSetReader,
		List<QuestionFactory<?>> factories
	) {
		this.questionSetEntityRepository = questionSetEntityRepository;
		this.teamRoleValidator = teamRoleValidator;
		this.lastViewedQuestionRedisRepository = lastViewedQuestionRedisRepository;
		this.reviewAnswerGrader = reviewAnswerGrader;
		this.questionReader = questionReader;
		this.questionSetReader = questionSetReader;
		questionFactories = factories.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionFactory::getQuestionType, Function.identity()));
	}

	public QuestionDto getLastViewedQuestionInReview(final Long questionSetId, final Long userId) {
		QuestionSetEntity questionSet = questionSetReader.getActiveQuestionSet(questionSetId);

		if (questionSet.getStatus() != QuestionSetStatus.REVIEW) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_REVIEW);
		}

		teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(questionSet.getTeamId(), userId);

		QuestionEntity lastViewedQuestion = lastViewedQuestionRedisRepository.getLastViewedQuestion(questionSet,
			userId);

		QuestionFactory<?> questionFactory = questionFactories.get(lastViewedQuestion.getType());
		return questionFactory.getQuestion(lastViewedQuestion, DeliveryMode.REVIEW.isAnswerVisible());
	}

	public void updateLastViewedQuestion(final Long questionSetId, final Long questionId, final Long userId) {
		QuestionEntity question = questionReader.getQuestion(questionId, questionSetId);

		lastViewedQuestionRedisRepository.updateLastViewedQuestion(question.getQuestionSet(), question, userId);
	}

	@Transactional(readOnly = true)
	public ReviewAnswerCheckResult checkReviewAnswer(final Long questionId, final Long questionSetId,
		final Long userId, final SubmitAnswerDto<?> submitAnswers) {
		final QuestionEntity question = questionReader.getQuestion(questionId, questionSetId);
		final QuestionSetEntity questionSet = questionSetReader.getActiveQuestionSet(questionSetId);

		if (questionSet.getVisibility() == QuestionSetVisibility.PRIVATE) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.NEED_OPEN);
		}

		if (questionSet.getVisibility() == QuestionSetVisibility.GROUP) {
			teamRoleValidator.checkHasSolveQuestionAuthorityInTeam(questionSet.getTeamId(), userId);
		}

		if (!questionSet.canReview()) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_REVIEW);
		}

		return reviewAnswerGrader.gradeAnswer(questionId, question, submitAnswers);
	}
}
