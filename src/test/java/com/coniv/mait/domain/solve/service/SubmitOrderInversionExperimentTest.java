package com.coniv.mait.domain.solve.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.coniv.mait.config.TestRedisConfig;
import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;

// 검증 latency 로 인한 submitOrder 역전이 어느 stagger(전송 간격)에서 발생하는지 측정하는 실험.
// 역전율이 0%가 되는 최소 stagger 가 임계값이며, 사람 반응속도·네트워크 지터보다 작으면 체감 불가.
@Disabled("순서 역전 측정 실험 - 로컬에서만 수동 실행")
@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@TestPropertySource(properties = {
	"spring.jpa.properties.hibernate.show_sql=false",
	"logging.level.org.hibernate.SQL=OFF",
	"logging.level.org.hibernate.orm.jdbc.bind=OFF"
})
class SubmitOrderInversionExperimentTest {

	private static final List<Long> STAGGER_MILLIS = List.of(0L, 1L, 2L, 5L, 10L, 20L, 50L);
	private static final int REPEAT_PER_STAGGER = 20;
	private static final List<Long> CORRECT_ANSWER = List.of(1L);

	@Autowired
	private QuestionAnswerSubmitService questionAnswerSubmitService;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private QuestionSetParticipantRepository questionSetParticipantRepository;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Autowired
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Autowired
	private QuestionScorerEntityRepository questionScorerEntityRepository;

	private QuestionSetEntity questionSet;
	private Long firstSenderId;
	private Long secondSenderId;

	@BeforeEach
	void setUp() {
		answerSubmitRecordEntityRepository.deleteAll();
		questionScorerEntityRepository.deleteAll();
		multipleChoiceEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetParticipantRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
		teamUserEntityRepository.deleteAll();
		userEntityRepository.deleteAll();

		UserEntity firstSender = userEntityRepository.save(
			UserEntity.localLoginUser("first@test.com", "password", "먼저보낸유저", "first"));
		UserEntity secondSender = userEntityRepository.save(
			UserEntity.localLoginUser("second@test.com", "password", "나중보낸유저", "second"));
		this.firstSenderId = firstSender.getId();
		this.secondSenderId = secondSender.getId();

		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("coniv", firstSender.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(firstSender, team));
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(secondSender, team));

		this.questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder().teamId(team.getId()).build());

		questionSetParticipantRepository.save(activeParticipant(firstSender));
		questionSetParticipantRepository.save(activeParticipant(secondSender));
	}

	@Test
	@DisplayName("stagger 별 submitOrder 역전율 측정")
	void measureInversionRateByStagger() {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		List<String> rows = new ArrayList<>();

		try {
			for (Long staggerMs : STAGGER_MILLIS) {
				int inversionCount = 0;
				for (int i = 0; i < REPEAT_PER_STAGGER; i++) {
					Long questionId = createFreshQuestion();
					if (isFirstSenderBeaten(executor, questionId, staggerMs)) {
						inversionCount++;
					}
				}
				double rate = (double)inversionCount / REPEAT_PER_STAGGER * 100;
				rows.add(String.format("%-12d %.0f%% (%d/%d)", staggerMs, rate, inversionCount, REPEAT_PER_STAGGER));
			}
		} finally {
			executor.shutdown();
		}

		printReport(rows);
	}

	private void printReport(List<String> rows) {
		String line = System.lineSeparator();
		StringBuilder report = new StringBuilder(line);
		report.append("==================== 역전 측정 결과 ====================").append(line);
		report.append("먼저 보낸 유저가 1등(최소 submitOrder)을 놓친 비율").append(line);
		report.append(String.format("%-12s %s", "stagger(ms)", "역전율")).append(line);
		rows.forEach(row -> report.append(row).append(line));
		report.append("=======================================================");
		System.out.println(report);
	}

	/**
	 * 먼저 보낸 유저(A)를 fire 한 뒤 stagger 만큼 지나 나중 유저(B)를 fire 한다.
	 * A 가 1등(가장 작은 submitOrder)을 놓치면 역전으로 판단한다.
	 */
	private boolean isFirstSenderBeaten(ExecutorService executor, Long questionId, Long staggerMs) {
		CompletableFuture<Void> first = CompletableFuture.runAsync(
			() -> submit(questionId, firstSenderId), executor);
		sleep(staggerMs);
		CompletableFuture<Void> second = CompletableFuture.runAsync(
			() -> submit(questionId, secondSenderId), executor);

		CompletableFuture.allOf(first, second).join();

		Map<Long, Long> orderByUserId = answerSubmitRecordEntityRepository.findAllByQuestionId(questionId).stream()
			.collect(Collectors.toMap(AnswerSubmitRecordEntity::getUserId, AnswerSubmitRecordEntity::getSubmitOrder));

		Long firstOrder = orderByUserId.get(firstSenderId);
		Long secondOrder = orderByUserId.get(secondSenderId);
		if (firstOrder == null || secondOrder == null) {
			return false;
		}
		return firstOrder > secondOrder;
	}

	private void submit(Long questionId, Long userId) {
		try {
			questionAnswerSubmitService.submitAnswer(
				questionSet.getId(), questionId, userId, new MultipleQuestionSubmitAnswer(CORRECT_ANSWER));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Long createFreshQuestion() {
		MultipleQuestionEntity question = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.number(1L)
				.questionSet(questionSet)
				.questionStatus(QuestionStatusType.SOLVE_PERMISSION)
				.lexoRank("m")
				.answerCount(2)
				.build());

		multipleChoiceEntityRepository.saveAll(List.of(
			MultipleChoiceEntity.builder().question(question).number(1).isCorrect(true).build(),
			MultipleChoiceEntity.builder().question(question).number(2).isCorrect(false).build()));

		return question.getId();
	}

	private QuestionSetParticipantEntity activeParticipant(UserEntity user) {
		return QuestionSetParticipantEntity.builder()
			.status(ParticipantStatus.ACTIVE)
			.questionSet(questionSet)
			.user(user)
			.winner(false)
			.build();
	}

	private void sleep(Long millis) {
		if (millis <= 0) {
			return;
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		}
	}
}
