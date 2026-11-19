package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.exception.QuestionException;
import com.coniv.mait.domain.question.exception.QuestionExceptionCode;
import com.coniv.mait.domain.question.external.AiCreateApiService;
import com.coniv.mait.domain.question.external.dto.AiCreateResponse;
import com.coniv.mait.domain.question.repository.AiRequestStatusManager;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.FillBlankQuestionFactory;
import com.coniv.mait.domain.question.service.component.MultipleQuestionFactory;
import com.coniv.mait.domain.question.service.component.OrderingQuestionFactory;
import com.coniv.mait.domain.question.service.component.QuestionReader;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.question.service.component.ShortQuestionFactory;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionCount;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.global.s3.service.S3FileUploader;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({QuestionService.class, MultipleQuestionFactory.class, ShortQuestionFactory.class,
	FillBlankQuestionFactory.class, OrderingQuestionFactory.class, QuestionImageService.class,
	TeamRoleValidator.class, TeamReader.class, UserReader.class, QuestionReader.class, QuestionSetReader.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuestionOrderConcurrencyIntegrationTest {

	@Autowired
	private QuestionReader questionReader;

	@Autowired
	private QuestionService service;
	@Autowired
	private QuestionSetEntityRepository questionSets;
	@Autowired
	private QuestionEntityRepository questions;
	@Autowired
	private MultipleChoiceEntityRepository choices;
	@Autowired
	private UserEntityRepository users;
	@Autowired
	private TeamEntityRepository teams;
	@Autowired
	private TeamUserEntityRepository members;
	@Autowired
	private PlatformTransactionManager transactionManager;

	@MockitoBean
	private AiCreateApiService aiCreateApiService;
	@MockitoBean
	private AiRequestStatusManager aiRequestStatusManager;
	@MockitoBean
	private S3FileUploader s3FileUploader;

	private Long userId;
	private Long questionSetId;
	private TransactionTemplate transaction;

	@BeforeEach
	void setUp() {
		transaction = new TransactionTemplate(transactionManager);
		transaction.executeWithoutResult(status -> {
			UserEntity user = users.save(
				UserEntity.localLoginUser(UUID.randomUUID() + "@test.com", "pw", "maker", null));
			TeamEntity team = teams.save(TeamEntity.ofGroup("순서 동시성 팀", user.getId()));
			members.save(TeamUserEntity.createTeamUser(user, team, TeamUserRole.MAKER));
			userId = user.getId();
			questionSetId = questionSets.save(QuestionSetEntity.builder().teamId(team.getId()).build()).getId();
		});
	}

	@Test
	@DisplayName("빈 문제셋에 동시 추가해도 문제와 선택지가 각각 저장되고 순서 키가 중복되지 않는다")
	void concurrentDefaultAdds_assignDistinctRanks() throws Exception {
		// given
		int count = 8;
		CountDownLatch ready = new CountDownLatch(count);
		CountDownLatch start = new CountDownLatch(1);

		// when
		try (var executor = Executors.newFixedThreadPool(count)) {
			List<Future<QuestionDto>> futures = IntStream.range(0, count).mapToObj(index ->
				executor.submit(() -> {
					ready.countDown();
					await(start);
					return service.createDefaultQuestion(questionSetId, userId);
				})).toList();
			try {
				assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			} finally {
				start.countDown();
			}
			for (Future<QuestionDto> future : futures) {
				assertThat(future.get(10, TimeUnit.SECONDS).getId()).isNotNull();
			}
		}

		// then
		List<QuestionEntity> saved = questions.findAllByQuestionSetId(questionSetId);
		assertThat(saved).hasSize(count).extracting(QuestionEntity::getLexoRank).doesNotHaveDuplicates();
		assertThat(choices.findAllByQuestionIdIn(saved.stream().map(QuestionEntity::getId).toList()))
			.hasSize(count * 4);
	}

	@Test
	@DisplayName("같은 이웃 사이의 동시 이동은 하나만 반영하고 최신 이웃으로 재시도할 수 있다")
	void concurrentMoves_rejectStaleGapAndAllowFreshRetry() throws Exception {
		// given
		List<Long> ids = transaction.execute(status -> List.of(saveQuestion("A"), saveQuestion("C"),
			saveQuestion("D"), saveQuestion("E")));
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		// when
		try (var executor = Executors.newFixedThreadPool(2)) {
			List<Future<Boolean>> futures = ids.subList(2, 4).stream().map(id -> executor.submit(() -> {
				ready.countDown();
				await(start);
				try {
					service.changeQuestionOrder(questionSetId, id, ids.get(0), ids.get(1), userId);
					return true;
				} catch (QuestionException ex) {
					assertThat(ex.getExceptionCode()).isEqualTo(QuestionExceptionCode.ORDER_CHANGED);
					return false;
				}
			})).toList();
			try {
				assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			} finally {
				start.countDown();
			}
			boolean firstMoved = futures.get(0).get(10, TimeUnit.SECONDS);
			assertThat(futures.get(1).get(10, TimeUnit.SECONDS)).isEqualTo(!firstMoved);

			Long movedId = ids.get(3);
			Long retryId = ids.get(2);
			if (firstMoved) {
				movedId = ids.get(2);
				retryId = ids.get(3);
			}
			service.changeQuestionOrder(questionSetId, retryId, ids.get(0), movedId, userId);

			// then
			assertThat(questionReader.getOrderedQuestions(questionSetId))
				.extracting(QuestionEntity::getId).containsExactly(ids.get(0), retryId, movedId, ids.get(1));
		}
	}

	@Test
	@DisplayName("문제 추가 후에도 외부 트랜잭션이 커밋할 때까지 다른 추가가 기다린다")
	void lock_isHeldUntilCommit() throws Exception {
		// given
		CountDownLatch created = new CountDownLatch(1);
		CountDownLatch commit = new CountDownLatch(1);
		CountDownLatch attempted = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Future<?> first = executor.submit(() -> transaction.executeWithoutResult(status -> {
				service.createDefaultQuestion(questionSetId, userId);
				created.countDown();
				await(commit);
			}));
			Future<?> second = null;
			try {
				await(created);
				second = executor.submit(() -> {
					attempted.countDown();
					return service.createDefaultQuestion(questionSetId, userId);
				});
				await(attempted);
				Future<?> waiting = second;

				// when & then
				assertThatThrownBy(() -> waiting.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
			} finally {
				commit.countDown();
			}
			first.get(10, TimeUnit.SECONDS);
			assertThat(second).isNotNull();
			second.get(10, TimeUnit.SECONDS);
		}
		assertThat(questions.findAllByQuestionSetId(questionSetId)).hasSize(2)
			.extracting(QuestionEntity::getLexoRank).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("문제와 선택지 저장이 롤백되면 잠금도 풀리고 다음 요청이 정상 저장된다")
	void rollback_releasesLockAndLeavesNoPartialData() throws Exception {
		// given
		transaction.executeWithoutResult(status -> {
			service.createDefaultQuestion(questionSetId, userId);
			status.setRollbackOnly();
		});
		assertThat(questions.findAllByQuestionSetId(questionSetId)).isEmpty();

		// when
		try (var executor = Executors.newSingleThreadExecutor()) {
			QuestionDto saved = executor.submit(() -> service.createDefaultQuestion(questionSetId, userId))
				.get(10, TimeUnit.SECONDS);

			// then
			assertThat(questions.findAllByQuestionSetId(questionSetId)).hasSize(1)
				.extracting(QuestionEntity::getId).containsExactly(saved.getId());
			assertThat(choices.findAllByQuestionId(saved.getId())).hasSize(4);
		}
	}

	@Test
	@DisplayName("일괄 기본 생성과 단건 추가는 같은 문제셋 잠금으로 순서 키를 할당한다")
	void batchAndSingleAdds_shareLock() throws Exception {
		// given
		QuestionSetEntity questionSet = questionSets.findById(questionSetId).orElseThrow();
		List<QuestionCount> counts = List.of(new QuestionCount(QuestionType.MULTIPLE, 3));

		// when
		try (var executor = Executors.newFixedThreadPool(2)) {
			List<Callable<Void>> tasks = List.of(
				() -> {
					service.createDefaultQuestions(questionSet, counts);
					return null;
				},
				() -> {
					service.createDefaultQuestion(questionSetId, userId);
					return null;
				});
			for (Future<Void> future : executor.invokeAll(tasks)) {
				future.get(10, TimeUnit.SECONDS);
			}
		}

		// then
		assertThat(questions.findAllByQuestionSetId(questionSetId)).hasSize(4)
			.extracting(QuestionEntity::getLexoRank).doesNotHaveDuplicates();
	}

	@Test
	@DisplayName("문제 순서는 대소문자를 구분하고 같은 키는 ID 순으로 조회한다")
	void orderedReads_useCaseSensitiveComparisonAndIdTieBreak() {
		// given
		List<Long> ids = transaction.execute(status ->
			List.of(saveQuestion("k"), saveQuestion("V"), saveQuestion("V")));

		// when & then
		assertThat(questionReader.getOrderedQuestions(questionSetId))
			.extracting(QuestionEntity::getId).containsExactly(ids.get(1), ids.get(2), ids.get(0));
		assertThat(service.getQuestions(questionSetId, DeliveryMode.MAKING))
			.extracting(QuestionDto::getId).containsExactly(ids.get(1), ids.get(2), ids.get(0));
	}

	@Test
	@DisplayName("AI 응답을 기다리는 동안 추가한 문제 뒤에 AI 문제를 저장한다")
	void aiGeneration_locksAfterExternalCallAndUsesLatestRank() throws Exception {
		// given
		CountDownLatch called = new CountDownLatch(1);
		CountDownLatch respond = new CountDownLatch(1);
		when(aiCreateApiService.createQuestionSet(any())).thenAnswer(invocation -> {
			called.countDown();
			await(respond);
			return new AiCreateResponse(List.of(MultipleQuestionDto.builder()
				.content("AI 문제").choices(List.of()).build()));
		});
		try (var executor = Executors.newSingleThreadExecutor()) {
			Future<?> generated = executor.submit(() -> service.createAiGeneratedQuestions(questionSetId,
				List.of(new QuestionCount(QuestionType.MULTIPLE, 1)), List.of(), "설명", "쉬움"));
			QuestionDto added;
			try {
				await(called);

				// when
				added = service.createDefaultQuestion(questionSetId, userId);
			} finally {
				respond.countDown();
			}
			generated.get(10, TimeUnit.SECONDS);

			// then
			assertThat(questionReader.getOrderedQuestions(questionSetId)).hasSize(2)
				.satisfies(saved -> assertThat(saved.getFirst().getId()).isEqualTo(added.getId()))
				.extracting(QuestionEntity::getLexoRank).doesNotHaveDuplicates();
		}
	}

	private Long saveQuestion(String rank) {
		return questions.save(MultipleQuestionEntity.builder().lexoRank(rank)
			.questionSet(questionSets.getReferenceById(questionSetId)).build()).getId();
	}

	private void await(CountDownLatch latch) {
		try {
			assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}
}
