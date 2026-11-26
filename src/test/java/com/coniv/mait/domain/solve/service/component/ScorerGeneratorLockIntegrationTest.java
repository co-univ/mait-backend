package com.coniv.mait.domain.solve.service.component;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.coniv.mait.domain.question.service.component.QuestionRedisKeys;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;

@SpringBootTest
@ActiveProfiles("test")
class ScorerGeneratorLockIntegrationTest {

	private static final String LOCK_KEY_PREFIX = "$lock:scorer:update:questionId:";

	@Autowired
	private ScorerGenerator scorerGenerator;

	@Autowired
	private QuestionScorerEntityRepository questionScorerEntityRepository;

	@Autowired
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Autowired
	private RedissonClient redissonClient;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private TransactionTemplate transactionTemplate;

	private Long questionId;

	@BeforeEach
	void setUp() {
		transactionTemplate = new TransactionTemplate(transactionManager);
		questionId = ThreadLocalRandom.current().nextLong(1_000_000L, 9_000_000L);
	}

	@AfterEach
	void tearDown() {
		questionScorerEntityRepository.findByQuestionId(questionId).ifPresent(questionScorerEntityRepository::delete);
		answerSubmitRecordEntityRepository.deleteAll(
			answerSubmitRecordEntityRepository.findAllByQuestionId(questionId));
		stringRedisTemplate.delete(QuestionRedisKeys.scorer(questionId));
	}

	@Test
	@DisplayName("득점자 갱신 락은 1초를 넘겨도 만료되지 않고 제출 트랜잭션 커밋 후에 해제된다")
	void updateScorer_holdsLockUntilCallerCommit() {
		// given & when
		transactionTemplate.executeWithoutResult(status -> {
			scorerGenerator.updateScorer(questionId, 1L, 1L);
			sleep(1_500);

			// then
			assertThat(acquirableByAnotherThread()).isFalse();
		});

		// then
		assertThat(acquirableByAnotherThread()).isTrue();
		assertThat(questionScorerEntityRepository.findByQuestionId(questionId))
			.hasValueSatisfying(scorer -> assertThat(scorer.getUserId()).isEqualTo(1L));
	}

	@Test
	@DisplayName("제출 트랜잭션이 롤백되면 득점자도 함께 롤백되고 락은 해제된다")
	void updateScorer_rollsBackWithCaller() {
		// when
		transactionTemplate.executeWithoutResult(status -> {
			scorerGenerator.updateScorer(questionId, 1L, 1L);
			status.setRollbackOnly();
		});

		// then
		assertThat(questionScorerEntityRepository.findByQuestionId(questionId)).isEmpty();
		assertThat(acquirableByAnotherThread()).isTrue();
	}

	@Test
	@DisplayName("같은 문제에 정답이 동시에 제출돼도 실패 없이 가장 빠른 제출자 한 명만 득점자로 남는다")
	void concurrentUpdateScorer_keepsFastestSubmitter() throws Exception {
		// given
		int submitters = 8;
		List<Long> submitOrders = new ArrayList<>(LongStream.rangeClosed(1, submitters).boxed().toList());
		Collections.shuffle(submitOrders);
		ExecutorService executor = Executors.newFixedThreadPool(submitters);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> results = new ArrayList<>();

		try {
			for (Long submitOrder : submitOrders) {
				results.add(executor.submit(() -> {
					start.await();
					transactionTemplate.executeWithoutResult(status ->
						scorerGenerator.updateScorer(questionId, 100L + submitOrder, submitOrder));
					return null;
				}));
			}

			// when
			start.countDown();

			// then
			for (Future<?> result : results) {
				assertThatCode(() -> result.get(30, TimeUnit.SECONDS)).doesNotThrowAnyException();
			}
		} finally {
			executor.shutdownNow();
		}
		assertThat(questionScorerEntityRepository.findAll().stream()
			.filter(scorer -> scorer.getQuestionId().equals(questionId)))
			.singleElement()
			.extracting(QuestionScorerEntity::getUserId, QuestionScorerEntity::getSubmitOrder)
			.containsExactly(101L, 1L);
	}

	@Test
	@DisplayName("득점자 재계산은 진행 중인 제출이 커밋될 때까지 기다린 뒤 그 제출을 반영한다")
	void recalculateScorer_waitsForInFlightSubmit() throws Exception {
		// given
		CountDownLatch submitInFlight = new CountDownLatch(1);
		CountDownLatch commitSubmit = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);

		try {
			Future<?> submit = executor.submit(() -> {
				transactionTemplate.executeWithoutResult(status -> {
					scorerGenerator.updateScorer(questionId, 1L, 1L);
					answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
						.userId(1L)
						.questionId(questionId)
						.isCorrect(true)
						.submitOrder(1L)
						.build());
					submitInFlight.countDown();
					await(commitSubmit);
				});
				return null;
			});
			assertThat(submitInFlight.await(5, TimeUnit.SECONDS)).isTrue();

			// when
			Future<?> recalculate = executor.submit(() -> scorerGenerator.recalculateScorer(questionId));

			// then
			sleep(500);
			assertThat(recalculate.isDone()).isFalse();

			commitSubmit.countDown();
			submit.get(10, TimeUnit.SECONDS);
			recalculate.get(10, TimeUnit.SECONDS);
		} finally {
			commitSubmit.countDown();
			executor.shutdownNow();
		}
		assertThat(questionScorerEntityRepository.findByQuestionId(questionId))
			.hasValueSatisfying(scorer -> assertThat(scorer)
				.extracting(QuestionScorerEntity::getUserId, QuestionScorerEntity::getSubmitOrder)
				.containsExactly(1L, 1L));
		assertThat(stringRedisTemplate.opsForValue().get(QuestionRedisKeys.scorer(questionId))).isEqualTo("1:1");
	}

	private boolean acquirableByAnotherThread() {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			return executor.submit(() -> {
				RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + questionId);
				boolean acquired = lock.tryLock(0, 5, TimeUnit.SECONDS);
				if (acquired) {
					lock.unlock();
				}
				return acquired;
			}).get(5, TimeUnit.SECONDS);
		} catch (Exception exception) {
			throw new IllegalStateException(exception);
		} finally {
			executor.shutdown();
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}

	private static void await(CountDownLatch latch) {
		try {
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}
}
