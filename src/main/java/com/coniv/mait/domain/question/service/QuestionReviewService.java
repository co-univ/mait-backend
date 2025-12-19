package com.coniv.mait.domain.question.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.LastViewedQuestionRedisRepository;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;

import jakarta.persistence.EntityNotFoundException;

@Service
public class QuestionReviewService {

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final TeamRoleValidator teamRoleValidator;

	private final QuestionReader questionReader;

	private final LastViewedQuestionRedisRepository lastViewedQuestionRedisRepository;

	private final Map<QuestionType, QuestionFactory<?>> questionFactories;

	@Autowired
	public QuestionReviewService(QuestionSetEntityRepository questionSetEntityRepository,
		TeamRoleValidator teamRoleValidator,
		LastViewedQuestionRedisRepository lastViewedQuestionRedisRepository,
		QuestionReader questionReader,
		List<QuestionFactory<?>> factories
	) {
		this.questionSetEntityRepository = questionSetEntityRepository;
		this.teamRoleValidator = teamRoleValidator;
		this.lastViewedQuestionRedisRepository = lastViewedQuestionRedisRepository;
		this.questionReader = questionReader;
		questionFactories = factories.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionFactory::getQuestionType, Function.identity()));
	}

	public QuestionDto getLastViewedQuestionInReview(final Long questionSetId, final Long userId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("해당 문제 셋을 조회할 수 없음"));

		if (questionSet.getOngoingStatus() != QuestionSetOngoingStatus.AFTER) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_AFTER);
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
}
