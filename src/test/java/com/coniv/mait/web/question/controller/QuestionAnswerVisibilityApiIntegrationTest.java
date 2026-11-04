package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;

@WithCustomUser
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuestionAnswerVisibilityApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private TransactionTemplate transactions;
	@Autowired
	private UserEntityRepository users;
	@Autowired
	private TeamEntityRepository teams;
	@Autowired
	private TeamUserEntityRepository memberships;
	@Autowired
	private QuestionSetEntityRepository questionSets;
	@Autowired
	private QuestionEntityRepository questions;
	@Autowired
	private MultipleChoiceEntityRepository choices;
	@Autowired
	private ShortAnswerEntityRepository shortAnswers;
	@Autowired
	private FillBlankAnswerEntityRepository blankAnswers;
	@Autowired
	private OrderingOptionEntityRepository options;

	@Autowired
	private SolvingSessionEntityRepository sessions;

	private Fixture fixture;

	private Long otherUserId;

	@AfterEach
	void tearDown() {
		if (fixture == null) {
			return;
		}
		transactions.executeWithoutResult(status -> {
			sessions.deleteAllByQuestionSetId(fixture.questionSetId());
			choices.deleteAllByQuestionIdIn(fixture.questionIds());
			shortAnswers.deleteAllByShortQuestionIdIn(fixture.questionIds());
			blankAnswers.deleteAllByFillBlankQuestionIdIn(fixture.questionIds());
			options.deleteAllByOrderingQuestionIdIn(fixture.questionIds());
			questions.deleteAllById(fixture.questionIds());
			questionSets.deleteById(fixture.questionSetId());
			memberships.deleteAllById(fixture.membershipIds());
			teams.deleteById(fixture.teamId());
			if (otherUserId != null) {
				users.deleteById(otherUserId);
			}
		});
	}

	@ParameterizedTest
	@MethodSource("playerRequests")
	@DisplayName("PLAYER가 mode와 타인 ID를 바꿔도 풀이 중 정답과 해설은 숨긴다")
	void playerCannotRevealAnswers(QuestionSetSolveMode solveMode, DeliveryMode mode) throws Exception {
		// given
		prepareFixture(solveMode, QuestionSetStatus.ONGOING, TeamUserRole.PLAYER);

		// when & then
		assertResponses(mode, false);
	}

	static Stream<Arguments> playerRequests() {
		return Stream.of(QuestionSetSolveMode.values()).flatMap(solveMode ->
			Stream.concat(Stream.of((DeliveryMode)null), Stream.of(DeliveryMode.values()))
				.map(mode -> Arguments.of(solveMode, mode)));
	}

	@ParameterizedTest
	@CsvSource({"STUDY, true, PROGRESSING, false", "STUDY, true, COMPLETE, true",
		"STUDY, false, COMPLETE, false", "LIVE_TIME, true, COMPLETE, false"})
	@DisplayName("학습모드에서는 본인의 채점 완료 세션만 정답 공개에 사용한다")
	void onlyOwnCompletedStudySessionRevealsAnswers(QuestionSetSolveMode solveMode, boolean own,
		SolvingStatus sessionStatus, boolean visible) throws Exception {
		// given
		prepareFixture(solveMode, QuestionSetStatus.ONGOING, TeamUserRole.PLAYER);
		Long requesterId = ((MaitUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
		transactions.executeWithoutResult(status -> {
			UserEntity user = own ? users.findById(requesterId).orElseThrow() : users.save(UserEntity.localLoginUser(
				"other-" + UUID.randomUUID() + "@test.com", "password", "다른 사용자", "other"));
			if (!own) {
				otherUserId = user.getId();
			}
			SolvingSessionEntity session = SolvingSessionEntity.studySession(user,
				questionSets.findById(fixture.questionSetId()).orElseThrow());
			if (sessionStatus == SolvingStatus.COMPLETE) {
				session.submit(4, 4);
			}
			sessions.save(session);
		});

		// when & then
		assertResponses(DeliveryMode.STUDY, visible);
	}

	@ParameterizedTest
	@CsvSource({"BEFORE", "AFTER"})
	@DisplayName("복습이 열리기 전에는 REVIEW 요청으로 정답을 공개할 수 없다")
	void playerCannotRevealBeforeReview(QuestionSetStatus status) throws Exception {
		// given
		prepareFixture(QuestionSetSolveMode.STUDY, status, TeamUserRole.PLAYER);

		// when & then
		assertResponses(DeliveryMode.REVIEW, false);
	}

	@ParameterizedTest
	@CsvSource({"MAKER, MAKING", "OWNER, MAKING", "MAKER, ONGOING", "OWNER, ONGOING", "PLAYER, REVIEW"})
	@DisplayName("관리자와 복습이 열린 문제셋의 PLAYER는 정답과 해설을 조회한다")
	void permittedUsersSeeAnswers(TeamUserRole role, QuestionSetStatus status) throws Exception {
		// given
		prepareFixture(QuestionSetSolveMode.STUDY, status, role);

		// when & then
		assertResponses(null, true);
	}

	@ParameterizedTest
	@MethodSource("allModes")
	@DisplayName("비멤버는 mode와 타인 ID를 바꿔도 단건과 목록 조회가 차단된다")
	void nonMemberCannotReadQuestions(DeliveryMode mode) throws Exception {
		// given
		prepareFixture(QuestionSetSolveMode.LIVE_TIME, QuestionSetStatus.ONGOING, null);

		// when & then
		assertForbidden(mode);
	}

	@ParameterizedTest
	@MethodSource("allModes")
	@DisplayName("제작 중인 문제는 PLAYER가 어떤 mode로도 읽을 수 없다")
	void playerCannotReadDrafts(DeliveryMode mode) throws Exception {
		// given
		prepareFixture(QuestionSetSolveMode.STUDY, QuestionSetStatus.MAKING, TeamUserRole.PLAYER);

		// when & then
		assertForbidden(mode);
	}

	static Stream<Arguments> allModes() {
		return Stream.concat(Stream.of((DeliveryMode)null), Stream.of(DeliveryMode.values()))
			.map(Arguments::of);
	}

	private void assertForbidden(DeliveryMode mode) throws Exception {
		mockMvc.perform(request(null, mode)).andExpectAll(status().isForbidden(), jsonPath("$.code").value("C-008"));
		for (Long questionId : fixture.questionIds()) {
			mockMvc.perform(request(questionId, mode))
				.andExpectAll(status().isForbidden(), jsonPath("$.code").value("C-008"));
		}
	}

	private void assertResponses(DeliveryMode mode, boolean visible) throws Exception {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		JsonNode list = data(request(null, mode));
		assertThat(list.size()).isEqualTo(4);
		for (JsonNode question : list) {
			assertQuestion(question, visible);
			if (mode == DeliveryMode.LIVE_TIME && fixture.solveMode() != QuestionSetSolveMode.LIVE_TIME) {
				mockMvc.perform(request(question.get("id").asLong(), mode)).andExpect(status().isBadRequest());
			} else {
				assertQuestion(data(request(question.get("id").asLong(), mode)), visible);
			}
		}
	}

	private JsonNode data(MockHttpServletRequestBuilder request) throws Exception {
		String response = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse()
			.getContentAsString();
		return objectMapper.readTree(response).get("data");
	}

	private void assertQuestion(JsonNode question, boolean visible) {
		assertThat(question.path("content").asText()).isEqualTo("공개 문제");
		if (visible) {
			assertThat(question.path("explanation").asText()).isEqualTo("비공개 해설");
		} else {
			assertThat(question.path("explanation").isNull() || question.path("explanation").isMissingNode()).isTrue();
		}
		switch (question.path("type").asText()) {
			case "SHORT", "FILL_BLANK" -> {
				if (visible) {
					assertThat(question.path("answers").get(0).path("answer").asText()).isEqualTo("비공개 정답");
				} else {
					assertThat(question.path("answers").isNull() || question.path("answers").isMissingNode()).isTrue();
				}
			}
			case "MULTIPLE" -> {
				assertThat(question.path("choices").get(0).path("content").asText()).isEqualTo("공개 보기");
				JsonNode correct = question.path("choices").get(0).path("isCorrect");
				assertThat(visible ? correct.asBoolean() : correct.isNull() || correct.isMissingNode()).isTrue();
			}
			case "ORDERING" -> {
				assertThat(question.path("options").get(0).path("content").asText()).isEqualTo("공개 보기");
				JsonNode answerOrder = question.path("options").get(0).path("answerOrder");
				assertThat(visible ? answerOrder.asInt() == 2
					: answerOrder.isNull() || answerOrder.isMissingNode()).isTrue();
			}
			default -> throw new AssertionError("지원하지 않는 문제 유형");
		}
	}

	private MockHttpServletRequestBuilder request(Long questionId, DeliveryMode mode) {
		String path = "/api/v1/question-sets/" + fixture.questionSetId() + "/questions";
		MockHttpServletRequestBuilder request = get(questionId == null ? path : path + "/" + questionId)
			.param("userId", otherUserId == null ? "999999999" : otherUserId.toString());
		return mode == null ? request : request.param("mode", mode.name());
	}

	private void prepareFixture(QuestionSetSolveMode solveMode, QuestionSetStatus state, TeamUserRole role) {
		Long requesterId = ((MaitUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
		fixture = transactions.execute(status -> {
			TeamEntity team = teams.save(TeamEntity.ofGroup("정답 공개 테스트", requesterId));
			List<Long> membershipIds = role == null ? List.of() : List.of(memberships.save(
				TeamUserEntity.createTeamUser(users.findById(requesterId).orElseThrow(), team, role)).getId());
			QuestionSetEntity set = questionSets.save(QuestionSetEntity.builder().title("정답 공개 테스트")
				.teamId(team.getId()).solveMode(solveMode).status(state).build());
			MultipleQuestionEntity multiple = questions.save(MultipleQuestionEntity.builder().questionSet(set)
				.lexoRank("a").number(1L).content("공개 문제").explanation("비공개 해설").answerCount(1).build());
			choices.save(MultipleChoiceEntity.builder().question(multiple).number(1).content("공개 보기")
				.isCorrect(true).build());
			ShortQuestionEntity shortQuestion = questions.save(ShortQuestionEntity.builder().questionSet(set)
				.lexoRank("b").number(2L).content("공개 문제").explanation("비공개 해설").answerCount(1).build());
			shortAnswers.save(ShortAnswerEntity.builder().shortQuestionId(shortQuestion.getId()).number(1L)
				.answer("비공개 정답").isMain(true).build());
			FillBlankQuestionEntity blank = questions.save(FillBlankQuestionEntity.builder().questionSet(set)
				.lexoRank("c").number(3L).content("공개 문제").explanation("비공개 해설").build());
			blankAnswers.save(FillBlankAnswerEntity.builder().fillBlankQuestionId(blank.getId()).number(1L)
				.answer("비공개 정답").isMain(true).build());
			OrderingQuestionEntity ordering = questions.save(OrderingQuestionEntity.builder().questionSet(set)
				.lexoRank("d").number(4L).content("공개 문제").explanation("비공개 해설").build());
			options.save(OrderingOptionEntity.builder().orderingQuestionId(ordering.getId()).originOrder(1)
				.content("공개 보기").answerOrder(2).build());
			return new Fixture(team.getId(), set.getId(),
				List.of(multiple.getId(), shortQuestion.getId(), blank.getId(), ordering.getId()),
				membershipIds, solveMode);
		});
	}

	private record Fixture(Long teamId, Long questionSetId, List<Long> questionIds, List<Long> membershipIds,
		QuestionSetSolveMode solveMode) {
	}
}
