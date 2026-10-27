package com.coniv.mait.global.interceptor.idempotency;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.service.component.QuestionRedisKeys;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

@WithCustomUser
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class IdempotencyIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository users;

	@Autowired
	private TeamEntityRepository teams;

	@Autowired
	private TeamUserEntityRepository memberships;

	@Autowired
	private QuestionSetParticipantRepository participants;

	@Autowired
	private QuestionSetEntityRepository questionSets;

	@Autowired
	private QuestionEntityRepository questions;

	@MockitoSpyBean
	private AnswerSubmitRecordEntityRepository submissions;

	@Autowired
	private TransactionTemplate transactions;

	@Autowired
	private StringRedisTemplate redis;

	private Fixture fixture;
	private String clientKey;
	private String body;

	@BeforeEach
	void setUp() {
		Long userId = ((MaitUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
		fixture = transactions.execute(status -> {
			UserEntity user = users.findById(userId).orElseThrow();
			TeamEntity team = teams.save(TeamEntity.ofGroup("idempotency-test", userId));
			TeamUserEntity membership = memberships.save(TeamUserEntity.createPlayerUser(user, team));
			QuestionSetEntity questionSet = questionSets.save(QuestionSetEntity.builder().teamId(team.getId()).build());
			QuestionSetParticipantEntity participant = participants.save(QuestionSetParticipantEntity.builder()
				.status(ParticipantStatus.ACTIVE).questionSet(questionSet).user(user).winner(false).build());
			MultipleQuestionEntity question = questions.save(MultipleQuestionEntity.builder().number(1L)
				.questionSet(questionSet).lexoRank("m").questionStatus(QuestionStatusType.SOLVE_PERMISSION).build());
			return new Fixture(userId, team.getId(), membership.getId(), questionSet.getId(), participant.getId(),
				question.getId());
		});
		clientKey = "test-" + UUID.randomUUID();
		body = "{\"type\":\"MULTIPLE\",\"userId\":" + userId + ",\"submitAnswers\":[1]}";
	}

	@AfterEach
	void tearDown() throws Exception {
		transactions.executeWithoutResult(status -> {
			submissions.deleteAllByQuestionIdIn(List.of(fixture.questionId()));
			participants.deleteById(fixture.participantId());
			questions.deleteById(fixture.questionId());
			questionSets.deleteById(fixture.questionSetId());
			memberships.deleteById(fixture.membershipId());
			teams.deleteById(fixture.teamId());
		});
		String scope = fixture.userId() + ":" + digest(path()) + ":" + digest(clientKey);
		redis.delete(List.of("$Idempotency:v2:" + scope, QuestionRedisKeys.submitOrder(fixture.questionId()),
			QuestionRedisKeys.submitFirstTime(fixture.questionId())));
	}

	@Test
	@DisplayName("동일한 정답 제출 요청을 재시도해도 DB에 한 번만 저장하고 동일 응답을 반환한다")
	void repeatedSubmissionReturnsSameResponseAndSingleRecord() throws Exception {
		// when
		MvcResult first = submit(body);
		MvcResult replay = submit(body);
		// then
		assertThat(replay.getResponse().getContentAsByteArray()).isEqualTo(first.getResponse().getContentAsByteArray());
		assertThat(replay.getResponse().getContentType()).isEqualTo(first.getResponse().getContentType());
		assertThat(submissions.findAllByQuestionId(fixture.questionId())).hasSize(1);
	}

	@Test
	@DisplayName("동시에 같은 답안을 제출해도 DB 기록과 성공 응답은 하나로 유지한다")
	void concurrentSubmissionsProduceOneRecord() throws Exception {
		// given
		int count = 8;
		CountDownLatch ready = new CountDownLatch(count);
		CountDownLatch start = new CountDownLatch(1);
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		try (var executor = Executors.newFixedThreadPool(count)) {
			List<Future<MvcResult>> requests = new ArrayList<>();
			for (int i = 0; i < count; i++) {
				requests.add(executor.submit(() -> {
					ready.countDown();
					if (!start.await(5, TimeUnit.SECONDS)) {
						throw new IllegalStateException("동시 시작 대기 시간 초과");
					}
					return mockMvc.perform(post(path()).with(authentication(auth))
						.header("Idempotency-Key", clientKey).contentType(MediaType.APPLICATION_JSON).content(body))
						.andReturn();
				}));
			}
			// when
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();
			List<String> successfulBodies = new ArrayList<>();
			for (Future<MvcResult> request : requests) {
				var response = request.get(10, TimeUnit.SECONDS).getResponse();
				assertThat(response.getStatus()).isIn(200, 409);
				if (response.getStatus() == 200) {
					successfulBodies.add(response.getContentAsString());
				} else {
					assertThat(objectMapper.readTree(response.getContentAsByteArray()).path("code").asText())
						.isEqualTo("C-005");
				}
			}
			// then
			assertThat(successfulBodies).isNotEmpty();
			assertThat(successfulBodies.stream().distinct()).hasSize(1);
			assertThat(submissions.findAllByQuestionId(fixture.questionId())).hasSize(1);
		}
	}

	@Test
	@DisplayName("실패 응답 직후 같은 키로 재시도하면 실제 제출을 수행한다")
	void failedRequestCanRetryImmediately() throws Exception {
		// given
		transactions.executeWithoutResult(status -> questions.findById(fixture.questionId()).orElseThrow()
			.updateQuestionStatus(QuestionStatusType.NOT_OPEN));
		// when
		mockMvc.perform(post(path()).header("Idempotency-Key", clientKey)
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest());
		assertThat(submissions.findAllByQuestionId(fixture.questionId())).isEmpty();
		transactions.executeWithoutResult(status -> questions.findById(fixture.questionId()).orElseThrow()
			.updateQuestionStatus(QuestionStatusType.SOLVE_PERMISSION));
		submit(body);
		// then
		assertThat(submissions.findAllByQuestionId(fixture.questionId())).hasSize(1);
	}

	@Test
	@DisplayName("서비스에서 저장 후 실패하면 DB 롤백 후 동일 키로 재시도할 수 있다")
	void rolledBackSubmissionCanRetry() throws Exception {
		// given
		var repositoryAnswer = mockingDetails(submissions).getMockCreationSettings().getDefaultAnswer();
		doAnswer(invocation -> {
			AnswerSubmitRecordEntity saved = (AnswerSubmitRecordEntity)repositoryAnswer.answer(invocation);
			assertThat(saved.getId()).isNotNull();
			throw new IllegalStateException("답안 저장 이후 실패");
		}).doAnswer(repositoryAnswer).when(submissions).save(any(AnswerSubmitRecordEntity.class));
		// when
		mockMvc.perform(post(path()).header("Idempotency-Key", clientKey)
				.contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isInternalServerError());
		// then
		assertThat(submissions.findAllByQuestionId(fixture.questionId())).isEmpty();
		submit(body);
		assertThat(submissions.findAllByQuestionId(fixture.questionId())).hasSize(1);
	}

	@Test
	@DisplayName("완료된 키로 다른 답안을 제출하면 캐시를 반환하지 않고 거절한다")
	void changedAnswerRejected() throws Exception {
		submit(body);
		mockMvc.perform(post(path()).header("Idempotency-Key", clientKey)
				.contentType(MediaType.APPLICATION_JSON).content(body.replace("[1]", "[2]")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("C-011"));
		assertThat(submissions.findAllByQuestionId(fixture.questionId())).hasSize(1);
	}

	private MvcResult submit(String content) throws Exception {
		return mockMvc.perform(post(path()).header("Idempotency-Key", clientKey)
				.contentType(MediaType.APPLICATION_JSON).content(content))
			.andExpect(status().isOk()).andReturn();
	}

	private String path() {
		return "/api/v1/question-sets/" + fixture.questionSetId() + "/questions/" + fixture.questionId() + "/submit";
	}

	private String digest(String value) throws Exception {
		return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
			.digest(value.getBytes(StandardCharsets.UTF_8)));
	}

	private record Fixture(Long userId, Long teamId, Long membershipId, Long questionSetId, Long participantId,
		Long questionId) {
	}
}
