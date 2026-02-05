package com.coniv.mait.domain.solve.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.coniv.mait.config.TestRedisConfig;
import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;

@Disabled("동시성 테스트는 로컬에서만 실행 - CI 환경에서 불안정")
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class SubmitAnswerConcurrencyTest {

	@Autowired
	private QuestionAnswerSubmitService questionAnswerSubmitService;

	@Autowired
	private QuestionScorerEntityRepository questionScorerRepository;

	@Autowired
	private AnswerSubmitRecordEntityRepository submitRecordRepository;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	private static final int THREAD_COUNT = 20;

	private Long questionSetId;
	private Long questionId;
	private List<Long> userIds;

	@BeforeEach
	void setUp() {
		// 기존 데이터 정리
		questionScorerRepository.deleteAll();
		submitRecordRepository.deleteAll();
		multipleChoiceEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
		userEntityRepository.deleteAll();

		// 테스트 데이터 생성
		setupTestData();
	}

	private void setupTestData() {
		// 1. QuestionSet 생성
		QuestionSetEntity questionSet = QuestionSetEntity.builder()
			.subject("테스트 과목")
			.title("동시성 테스트 문제집")
			.teamId(1L)
			.build();
		questionSet = questionSetEntityRepository.save(questionSet);
		this.questionSetId = questionSet.getId();

		// 2. MultipleQuestion 생성
		MultipleQuestionEntity question = MultipleQuestionEntity.builder()
			.content("2 + 2 = ?")
			.explanation("간단한 덧셈 문제입니다.")
			.number(1L)
			.questionStatus(QuestionStatusType.SOLVE_PERMISSION)
			.questionSet(questionSet)
			.answerCount(4)
			.build();
		question = questionEntityRepository.save(question);
		this.questionId = question.getId();

		// 3. 선택지 생성 (정답: 1번 선택지)
		List<MultipleChoiceEntity> choices = List.of(
			MultipleChoiceEntity.builder()
				.question(question)
				.content("4")
				.isCorrect(true)
				.number(1)
				.build(),
			MultipleChoiceEntity.builder()
				.question(question)
				.content("3")
				.isCorrect(false)
				.number(2)
				.build(),
			MultipleChoiceEntity.builder()
				.question(question)
				.content("5")
				.isCorrect(false)
				.number(3)
				.build(),
			MultipleChoiceEntity.builder()
				.question(question)
				.content("6")
				.isCorrect(false)
				.number(4)
				.build()
		);
		multipleChoiceEntityRepository.saveAll(choices);

		// 4. 테스트 사용자들 생성
		List<UserEntity> users = LongStream.rangeClosed(1, THREAD_COUNT)
			.mapToObj(i -> UserEntity.localLoginUser(
				"test" + i + "@example.com",
				"password",
				"테스트유저" + i,
				"nickname" + i
			))
			.toList();
		users = userEntityRepository.saveAll(users);
		this.userIds = users.stream().map(UserEntity::getId).toList();
	}

	@Test
	@DisplayName("멀티스레드_동시성_정답자_1명만_저장되는지_검증")
	void multi_thread_concurrencyTest() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

		for (Long userId : userIds) {
			executor.submit(() -> {
				try {
					// 정답 제출 (선택지 1번이 정답)
					questionAnswerSubmitService.submitAnswer(
						questionSetId,
						questionId,
						userId,
						new MultipleQuestionSubmitAnswer(List.of(1L)) // 정답 선택지 번호
					);
				} catch (Exception e) {
					System.err.println("사용자 " + userId + " 제출 중 오류: " + e.getMessage());
					e.printStackTrace();
				} finally {
					latch.countDown();
				}
			});
		}

		// 모든 스레드가 완료될 때까지 대기
		boolean completed = latch.await(30, TimeUnit.SECONDS);
		assertThat(completed).isTrue();

		// 잠시 대기 (비동기 처리 완료를 위해)
		Thread.sleep(1000);

		// --- 검증 ---
		// 1. 득점자는 정확히 1명만 있어야 함
		List<QuestionScorerEntity> scorers = questionScorerRepository.findAll();
		System.out.println("총 득점자 수: " + scorers.size());

		// 모든 득점자 정보 출력
		for (int i = 0; i < scorers.size(); i++) {
			QuestionScorerEntity scorer = scorers.get(i);
			System.out.println(
				"득점자[" + i + "] userId = " + scorer.getUserId() + ", order = " + scorer.getSubmitOrder());
		}

		if (scorers.size() == 1) {
			QuestionScorerEntity scorer = scorers.get(0);

			// 득점자가 실제로 존재하는 사용자인지 확인
			assertThat(userIds).contains(scorer.getUserId());

			// 제출 순서가 1 이상이어야 함
			assertThat(scorer.getSubmitOrder()).isGreaterThan(0);
		}

		assertThat(scorers).hasSize(1);

		// 2. 모든 사용자의 제출 기록이 있어야 함
		List<AnswerSubmitRecordEntity> records = submitRecordRepository.findAllByQuestionId(questionId);
		System.out.println("총 제출 기록 수: " + records.size());
		assertThat(records).hasSize(THREAD_COUNT);

		// 3. 모든 제출이 정답이어야 함
		long correctCount = records.stream().mapToLong(record -> record.isCorrect() ? 1 : 0).sum();
		System.out.println("정답 제출 수: " + correctCount);
		assertThat(correctCount).isEqualTo(THREAD_COUNT);

		// 4. 제출 순서가 중복되지 않아야 함
		List<Long> submitOrders = records.stream().map(AnswerSubmitRecordEntity::getSubmitOrder).toList();
		long uniqueOrderCount = submitOrders.stream().distinct().count();
		System.out.println("고유한 제출 순서 수: " + uniqueOrderCount);
		assertThat(uniqueOrderCount).isEqualTo(THREAD_COUNT);

		executor.shutdown();
	}
}
