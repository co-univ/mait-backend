package com.coniv.mait.domain.question.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.event.AiQuestionGenerationRequestedEvent;
import com.coniv.mait.domain.question.dto.MaterialDto;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.AiRequestStatus;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.repository.AiRequestStatusManager;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionChecker;
import com.coniv.mait.domain.question.service.dto.QuestionCount;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.web.question.dto.QuestionSetContainer;
import com.coniv.mait.web.question.dto.QuestionSetGroup;
import com.coniv.mait.web.question.dto.QuestionSetList;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionSetService {

	private final QuestionService questionService;

	private final ApplicationEventPublisher eventPublisher;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final QuestionEntityRepository questionEntityRepository;

	private final QuestionChecker questionChecker;

	private final TeamRoleValidator teamRoleValidator;

	private final QuestionSetMaterialService questionSetMaterialService;

	private final AiRequestStatusManager aiRequestStatusManager;

	@Transactional
	public QuestionSetDto createQuestionSet(final QuestionSetDto questionSetDto, final List<QuestionCount> counts,
		final List<MaterialDto> materials, final String instruction, final String difficulty, final Long userId) {
		teamRoleValidator.checkHasCreateQuestionSetAuthority(questionSetDto.getTeamId(), userId);

		QuestionSetEntity questionSetEntity = QuestionSetEntity.builder()
			.subject(questionSetDto.getSubject())
			.creationType(questionSetDto.getCreationType())
			.teamId(questionSetDto.getTeamId())
			.difficulty(difficulty)
			.creatorId(userId)
			.build();
		questionSetEntityRepository.save(questionSetEntity);
		questionSetMaterialService.updateUsed(materials);

		if (questionSetDto.getCreationType() == QuestionSetCreationType.MANUAL) {
			questionService.createDefaultQuestions(questionSetEntity, counts);
		}

		if (questionSetDto.getCreationType() == QuestionSetCreationType.AI_GENERATED) {
			eventPublisher.publishEvent(new AiQuestionGenerationRequestedEvent(
				questionSetEntity.getId(),
				counts,
				materials,
				instruction,
				difficulty));
		}

		return QuestionSetDto.from(questionSetEntity);
	}

	public QuestionSetContainer getQuestionSets(final Long teamId, final DeliveryMode mode) {
		// Todo: 조회하려는 유저와 팀이 일치하는지 확인
		List<QuestionSetDto> questionSets = questionSetEntityRepository.findAllByTeamIdAndDeliveryMode(teamId, mode)
			.stream()
			.sorted(Comparator.comparing(
				QuestionSetEntity::getModifiedAt,
				Comparator.nullsLast(Comparator.naturalOrder())).reversed())
			.map(QuestionSetDto::from)
			.toList();

		if (mode == DeliveryMode.MAKING || mode == DeliveryMode.REVIEW) {
			return QuestionSetList.of(questionSets);
		}

		return QuestionSetGroup.of(questionSets);
	}

	public QuestionSetDto getQuestionSet(final Long questionSetId) {
		final QuestionSetEntity questionSetEntity = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new IllegalArgumentException("Question set not found"));

		long questionCount = questionEntityRepository.countByQuestionSetId(questionSetEntity.getId());

		return QuestionSetDto.of(questionSetEntity, questionCount);
	}

	@Transactional
	public QuestionSetDto completeQuestionSet(
		final Long questionSetId,
		final String title,
		final String subject,
		final DeliveryMode mode,
		final String difficulty,
		final QuestionSetVisibility visibility) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("Question set not found"));

		// Todo: 현재 생성 단계가 아니면 예외
		int number = 1;

		List<QuestionEntity> questions = questionEntityRepository.findAllByQuestionSetIdOrderByLexoRankAsc(
			questionSetId);
		for (QuestionEntity question : questions) {
			question.updateNumber(number++);
		}

		questionSet.completeQuestionSet(title, subject, mode, difficulty, visibility);
		return QuestionSetDto.from(questionSet);
	}

	@Transactional
	public void updateQuestionSetField(final Long questionSetId, final String title) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("Question set not found"));

		questionSet.updateTitle(title);
	}

	public List<QuestionValidateDto> validateQuestionSet(final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("Question set not found"));

		List<QuestionEntity> questions = questionEntityRepository.findAllByQuestionSetId(questionSet.getId());

		return questions.stream()
			.map(questionChecker::validateQuestion)
			.filter(dto -> !dto.isValid())
			.toList();
	}

	public AiRequestStatus getAiRequestStatus(Long questionSetId) {
		return aiRequestStatusManager.getStatus(questionSetId);
	}

	@Transactional
	public void updateQuestionSetToReviewMode(final Long questionSetId, final QuestionSetVisibility visibility) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("해당 문제 셋을 찾을 수 없습니다."));

		if (questionSet.getOngoingStatus() != QuestionSetOngoingStatus.AFTER) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_AFTER);
		}

		questionSet.updateMode(DeliveryMode.REVIEW);
		questionSet.updateVisibility(visibility);
	}
}
