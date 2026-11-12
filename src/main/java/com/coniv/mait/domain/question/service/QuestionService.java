package com.coniv.mait.domain.question.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.question.dto.MaterialDto;
import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.AiRequestStatus;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionException;
import com.coniv.mait.domain.question.exception.QuestionExceptionCode;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.external.AiCreateApiService;
import com.coniv.mait.domain.question.external.dto.AiCreateRequest;
import com.coniv.mait.domain.question.external.dto.AiCreateResponse;
import com.coniv.mait.domain.question.repository.AiRequestStatusManager;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionFactory;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.question.service.dto.CurrentQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionCount;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.question.util.LexoRank;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;
import com.coniv.mait.global.exception.custom.UserParameterException;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class QuestionService {

	private static final QuestionType DEFAULT_QUESTION_TYPE = QuestionType.MULTIPLE;

	private static final int MAX_RANK_LENGTH = 255;

	private final QuestionEntityRepository questionEntityRepository;

	private final QuestionSetEntityRepository questionSetEntityRepository;

	private final Map<QuestionType, QuestionFactory<?>> questionFactories;

	private final QuestionImageService questionImageService;

	private final MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	private final AiCreateApiService aiCreateApiService;

	private final AiRequestStatusManager aiRequestStatusManager;

	private final TeamRoleValidator teamRoleValidator;

	private final QuestionReader questionReader;

	private final QuestionSetReader questionSetReader;

	@Autowired
	public QuestionService(
		List<QuestionFactory<?>> factories,
		QuestionEntityRepository questionEntityRepository,
		QuestionSetEntityRepository questionSetEntityRepository,
		MultipleChoiceEntityRepository multipleChoiceEntityRepository,
		QuestionImageService questionImageService,
		AiCreateApiService aiCreateApiService,
		AiRequestStatusManager aiRequestStatusManager,
		TeamRoleValidator teamRoleValidator,
		QuestionReader questionReader,
		QuestionSetReader questionSetReader) {
		questionFactories = factories.stream()
			.collect(Collectors.toUnmodifiableMap(QuestionFactory::getQuestionType, Function.identity()));
		this.questionEntityRepository = questionEntityRepository;
		this.questionSetEntityRepository = questionSetEntityRepository;
		this.questionImageService = questionImageService;
		this.multipleChoiceEntityRepository = multipleChoiceEntityRepository;
		this.aiCreateApiService = aiCreateApiService;
		this.aiRequestStatusManager = aiRequestStatusManager;
		this.teamRoleValidator = teamRoleValidator;
		this.questionReader = questionReader;
		this.questionSetReader = questionSetReader;
	}

	public <T extends QuestionDto> void createQuestion(
		final Long questionSetId,
		final QuestionType type,
		final T questionDto,
		final Long userId
	) {
		QuestionSetEntity questionSetEntity = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));
		teamRoleValidator.checkIsTeamManager(questionSetEntity.getTeamId(), userId);

		QuestionFactory<QuestionDto> questionFactory = getQuestionFactory(type);

		questionFactory.save(questionDto, questionSetEntity);
	}

	@Transactional
	public QuestionDto createDefaultQuestion(final Long questionSetId, final Long userId) {
		QuestionSetEntity questionSet = questionSetReader.getQuestionSetForUpdate(questionSetId);
		teamRoleValidator.checkIsTeamManager(questionSet.getTeamId(), userId);

		final String nextRank = nextRank(questionReader.getOrderedQuestionsForUpdate(questionSetId));

		QuestionEntity defaultQuestion = QuestionEntity.createDefaultQuestion(questionSet, nextRank);
		questionEntityRepository.save(defaultQuestion);

		List<MultipleChoiceEntity> defaultSubEntities = QuestionFactory.createDefaultSubEntities(
			(MultipleQuestionEntity)defaultQuestion);
		multipleChoiceEntityRepository.saveAll(defaultSubEntities);

		return getQuestionFactory(DEFAULT_QUESTION_TYPE).getQuestion(defaultQuestion, true);
	}

	@Transactional
	public void createDefaultQuestions(final QuestionSetEntity questionSetEntity, final List<QuestionCount> counts) {
		questionSetReader.getQuestionSetForUpdate(questionSetEntity.getId());
		String currentRank = nextRank(questionReader.getOrderedQuestionsForUpdate(questionSetEntity.getId()));

		for (QuestionCount count : counts) {
			QuestionFactory<QuestionDto> questionFactory = getQuestionFactory(count.type());
			for (int i = 0; i < count.count(); i++) {
				validateRank(currentRank, null, null);
				questionFactory.createDefaultQuestion(currentRank, questionSetEntity);
				currentRank = LexoRank.nextAfter(currentRank);
			}
		}
	}

	@Transactional(readOnly = true)
	public QuestionDto getQuestion(final Long questionSetId, final Long questionId, final DeliveryMode mode) {
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("해당 문제 셋에 속한 문제가 아닙니다.");
		}

		if (mode == DeliveryMode.LIVE_TIME && !question.getQuestionSet().isOnLive()) {
			throw new QuestionSetStatusException(QuestionSetStatusExceptionCode.ONLY_LIVE_TIME);
		}

		boolean answerVisible = mode == null || mode.isAnswerVisible();

		return questionFactories.get(question.getType()).getQuestion(question, answerVisible);
	}

	// Todo: 조회 성능 개선
	public List<QuestionDto> getQuestions(final Long questionSetId, final DeliveryMode mode) {
		List<QuestionEntity> questions = questionEntityRepository.findAllByQuestionSetId(questionSetId);
		return questions.stream()
			.sorted(mode.questionComparator())
			.map(question -> getQuestionFactory(question.getType()).getQuestion(question, mode.isAnswerVisible()))
			.toList();
	}

	@SuppressWarnings("unchecked")
	private <T extends QuestionDto> QuestionFactory<T> getQuestionFactory(
		final QuestionType type
	) {
		QuestionFactory<T> factory = (QuestionFactory<T>)questionFactories.get(type);
		if (factory == null) {
			throw new IllegalArgumentException("지원하지 않는 문제 타입입니다: " + type);
		}
		return factory;
	}

	public CurrentQuestionDto findCurrentQuestion(final Long questionSetId) {
		QuestionSetEntity questionSet = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));

		Optional<QuestionEntity> mayBeOpenQuestion = questionEntityRepository.findFirstByQuestionSetAndQuestionStatusIn(
			questionSet, QuestionStatusType.openStatuses());

		if (mayBeOpenQuestion.isPresent()) {
			QuestionEntity question = mayBeOpenQuestion.get();
			return CurrentQuestionDto.of(questionSetId, question.getId(), question.getQuestionStatus());
		}

		return CurrentQuestionDto.notOpenQuestion(questionSetId);
	}

	@Transactional
	public QuestionDto updateQuestion(final Long questionSetId, final Long questionId, final QuestionDto questionDto,
		final Long userId) {
		questionSetReader.getQuestionSetForUpdate(questionSetId);
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("해당 문제 셋에 속한 문제가 아닙니다.");
		}
		teamRoleValidator.checkIsTeamManager(question.getQuestionSet().getTeamId(), userId);

		QuestionFactory<QuestionDto> questionFactory = getQuestionFactory(questionDto.getType());

		if (question.getImageId() != null && !question.getImageId().equals(questionDto.getImageId())) {
			questionImageService.unUseExistImage(question.getImageId());
		}

		if (question.getType() == questionDto.getType()) {
			question.updateContent(questionDto.getContent());
			question.updateExplanation(questionDto.getExplanation());
			question.updateImage(questionDto.getImageUrl(), questionDto.getImageId());

			questionFactory.deleteSubEntities(question);
			questionFactory.createSubEntities(questionDto, question);
			return questionFactory.getQuestion(question, true);
		}

		QuestionFactory<QuestionDto> oldQuestionFactory = getQuestionFactory(question.getType());
		oldQuestionFactory.deleteSubEntities(question);
		questionEntityRepository.delete(question);

		QuestionEntity createdQuestion = questionFactory.create(questionDto, question.getQuestionSet());

		createdQuestion.updateImage(questionDto.getImageUrl(), questionDto.getImageId());
		createdQuestion.updateLexoRank(question.getLexoRank());
		questionEntityRepository.save(createdQuestion);
		questionFactory.createSubEntities(questionDto, createdQuestion);

		return questionFactory.getQuestion(createdQuestion, true);
	}

	@Transactional
	public void deleteQuestion(final Long questionSetId, final Long questionId, final Long userId) {
		questionSetReader.getQuestionSetForUpdate(questionSetId);
		QuestionEntity question = questionEntityRepository.findById(questionId)
			.orElseThrow(() -> new EntityNotFoundException("Question not found with id: " + questionId));

		if (!question.getQuestionSet().getId().equals(questionSetId)) {
			throw new ResourceNotBelongException("해당 문제 셋에 속한 문제가 아닙니다.");
		}
		teamRoleValidator.checkIsTeamManager(question.getQuestionSet().getTeamId(), userId);

		QuestionFactory<?> questionFactory = questionFactories.get(question.getType());
		questionFactory.deleteSubEntities(question);
		questionEntityRepository.deleteById(question.getId());
	}

	/**
	 * 잠금 후 최신 이웃을 검증한다. 위치 충돌 시 목록을 다시 조회해 재요청해야 한다.
	 * 간격 부족·동률·255자 초과는 저장을 거절하며, 실제 키 재배치는 EDU-180에서 처리한다.
	 */
	@Transactional
	public void changeQuestionOrder(final Long questionSetId, final Long sourceQuestionId, final Long prevQuestionId,
		final Long nextQuestionId, final Long userId) {
		QuestionSetEntity questionSet = questionSetReader.getQuestionSetForUpdate(questionSetId);
		teamRoleValidator.checkIsTeamManager(questionSet.getTeamId(), userId);
		List<QuestionEntity> questions = questionReader.getOrderedQuestionsForUpdate(questionSetId);
		QuestionEntity source = questionReader.getQuestion(sourceQuestionId, questionSetId);
		if (!questions.contains(source)) {
			throw new QuestionException(QuestionExceptionCode.ORDER_CHANGED);
		}

		if (Objects.equals(sourceQuestionId, prevQuestionId) || Objects.equals(sourceQuestionId, nextQuestionId)
			|| (prevQuestionId != null && prevQuestionId.equals(nextQuestionId))) {
			throw new UserParameterException("이동할 문제와 앞뒤 문제는 서로 달라야 합니다.");
		}

		List<QuestionEntity> remaining = questions.stream()
			.filter(question -> !question.getId().equals(sourceQuestionId)).toList();
		QuestionEntity prev = null;
		QuestionEntity next = null;
		int prevIndex = -1;
		int nextIndex = remaining.size();
		if (prevQuestionId != null) {
			prev = questionReader.getQuestion(prevQuestionId, questionSetId);
			prevIndex = remaining.indexOf(prev);
			if (prevIndex < 0) {
				throw new QuestionException(QuestionExceptionCode.ORDER_CHANGED);
			}
		}
		if (nextQuestionId != null) {
			next = questionReader.getQuestion(nextQuestionId, questionSetId);
			nextIndex = remaining.indexOf(next);
			if (nextIndex < 0) {
				throw new QuestionException(QuestionExceptionCode.ORDER_CHANGED);
			}
		}
		if (nextIndex != prevIndex + 1) {
			throw new QuestionException(QuestionExceptionCode.ORDER_CHANGED);
		}

		String prevRank = null;
		String nextRank = null;
		if (prev != null) {
			prevRank = prev.getLexoRank();
		}
		if (next != null) {
			nextRank = next.getLexoRank();
		}
		if ((prev != null && (prevRank == null || prevRank.isBlank()))
			|| (next != null && (nextRank == null || nextRank.isBlank()))
			|| (prevRank != null && nextRank != null && prevRank.compareTo(nextRank) >= 0)) {
			throw new QuestionException(QuestionExceptionCode.ORDER_REBALANCE_REQUIRED);
		}

		String newRank = LexoRank.between(prevRank, nextRank);
		validateRank(newRank, prevRank, nextRank);
		if (remaining.stream().anyMatch(question -> newRank.equals(question.getLexoRank()))) {
			throw new QuestionException(QuestionExceptionCode.ORDER_REBALANCE_REQUIRED);
		}
		source.updateRank(newRank);
	}

	private String nextRank(final List<QuestionEntity> questions) {
		if (questions.isEmpty()) {
			return LexoRank.middle();
		}
		String lastRank = questions.getLast().getLexoRank();
		if (lastRank == null || lastRank.isBlank()) {
			throw new QuestionException(QuestionExceptionCode.ORDER_REBALANCE_REQUIRED);
		}
		String rank = LexoRank.nextAfter(lastRank);
		validateRank(rank, lastRank, null);
		return rank;
	}

	private void validateRank(final String rank, final String prevRank, final String nextRank) {
		if (rank.length() > MAX_RANK_LENGTH || (prevRank != null && rank.compareTo(prevRank) <= 0)
			|| (nextRank != null && rank.compareTo(nextRank) >= 0)) {
			throw new QuestionException(QuestionExceptionCode.ORDER_REBALANCE_REQUIRED);
		}
	}

	@Async("aiUploadThreadPoolExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void createAiGeneratedQuestions(
		Long questionSetId,
		List<QuestionCount> counts,
		List<MaterialDto> materials,
		String instruction,
		String difficulty
	) {
		aiRequestStatusManager.updateStatus(questionSetId, AiRequestStatus.PENDING);
		log.info("[AI 문제 생성 시작] - QuestionSetId: {}, Status: PROCESSING", questionSetId);

		QuestionSetEntity questionSetEntity = questionSetEntityRepository.findById(questionSetId)
			.orElseThrow(() -> new EntityNotFoundException("QuestionSet not found with id: " + questionSetId));

		AiCreateRequest aiRequest = AiCreateRequest.builder()
			.title(questionSetEntity.getTitle())
			.urls(materials.stream().map(MaterialDto::getUrl).toList())
			.instruction(instruction)
			.difficulty(difficulty)
			.counts(counts.stream()
				.collect(
					Collectors.toUnmodifiableMap(
						cc -> cc.type().name(),
						QuestionCount::count))
			)
			.build();

		try {
			AiCreateResponse createdQuestions = aiCreateApiService.createQuestionSet(aiRequest);
			log.info("response: {}", createdQuestions.getContent());
			aiRequestStatusManager.updateStatus(questionSetId, AiRequestStatus.PROCESSING);
			List<QuestionDto> questions = createdQuestions.getContent();

			// 외부 AI 호출을 마친 뒤 잠금을 잡아 기본 추가·이동과 순서 할당을 직렬화한다.
			questionSetReader.getQuestionSetForUpdate(questionSetId);
			String currentRank = nextRank(questionReader.getOrderedQuestionsForUpdate(questionSetId));

			for (QuestionDto questionDto : questions) {
				QuestionFactory<QuestionDto> questionFactory = getQuestionFactory(questionDto.getType());
				QuestionEntity question = questionFactory.create(questionDto, questionSetEntity);
				validateRank(currentRank, null, null);
				question.updateLexoRank(currentRank);

				questionEntityRepository.save(question);
				questionFactory.createSubEntities(questionDto, question);
				currentRank = LexoRank.nextAfter(currentRank);
			}

			aiRequestStatusManager.updateStatus(questionSetId, AiRequestStatus.COMPLETED);
			log.info("[AI 문제 생성 완료] - QuestionSetId: {}, 문제 수: {}", questionSetId, questions.size());

		} catch (Exception e) {
			log.error("[AI 문제 생성 실패] - QuestionSetId: {}", questionSetId, e);
			aiRequestStatusManager.updateStatus(questionSetId, AiRequestStatus.FAILED);
			throw new RuntimeException("AI 문제 생성 중 오류 발생", e);
		}
	}
}
