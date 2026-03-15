package com.coniv.mait.web.question.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import com.coniv.mait.config.TestRedisConfig;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

@WithCustomUser
@Import(TestRedisConfig.class)
public class TeamQuestionRankApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Autowired
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	@BeforeEach
	void setUp() {
		solvingSessionEntityRepository.deleteAll();
		answerSubmitRecordEntityRepository.deleteAll();
		teamUserEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("개인 정답률 조회 API 통합 테스트 - 정상 케이스")
	void getPersonalAccuracy_Success() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();

		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("테스트 팀").creatorId(currentUser.getId()).build());

		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(currentUser, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("테스트 문제집")
				.teamId(team.getId())
				.deliveryMode(DeliveryMode.LIVE_TIME)
				.ongoingStatus(QuestionSetOngoingStatus.AFTER)
				.build());

		MultipleQuestionEntity q1 = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet).content("문제 1").number(1L).lexoRank("a").build());

		MultipleQuestionEntity q2 = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet).content("문제 2").number(2L).lexoRank("b").build());

		MultipleQuestionEntity q3 = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet).content("문제 3").number(3L).lexoRank("c").build());

		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(currentUser.getId()).questionId(q1.getId())
			.submitOrder(1L).isCorrect(true).submittedAnswer("{}").build());

		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(currentUser.getId()).questionId(q2.getId())
			.submitOrder(2L).isCorrect(true).submittedAnswer("{}").build());

		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(currentUser.getId()).questionId(q3.getId())
			.submitOrder(3L).isCorrect(false).submittedAnswer("{}").build());

		// when & then
		mockMvc.perform(get("/api/v1/teams/{teamId}/user-solving-stats", team.getId())
				.accept(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.totalSolvedCount").value(3),
				jsonPath("$.data.correctCount").value(2),
				jsonPath("$.data.accuracyRate").value(66.7)
			);
	}

	@Test
	@DisplayName("개인 정답률 조회 API 통합 테스트 - 재제출로 정답 처리")
	void getPersonalAccuracy_RetryCorrect() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();

		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("테스트 팀").creatorId(currentUser.getId()).build());

		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(currentUser, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("테스트 문제집")
				.teamId(team.getId())
				.deliveryMode(DeliveryMode.LIVE_TIME)
				.ongoingStatus(QuestionSetOngoingStatus.AFTER)
				.build());

		MultipleQuestionEntity q1 = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet).content("문제 1").number(1L).lexoRank("a").build());

		// 오답 → 정답 (재제출)
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(currentUser.getId()).questionId(q1.getId())
			.submitOrder(1L).isCorrect(false).submittedAnswer("{}").build());

		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(currentUser.getId()).questionId(q1.getId())
			.submitOrder(2L).isCorrect(true).submittedAnswer("{}").build());

		// when & then
		mockMvc.perform(get("/api/v1/teams/{teamId}/user-solving-stats", team.getId())
				.accept(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.totalSolvedCount").value(1),
				jsonPath("$.data.correctCount").value(1),
				jsonPath("$.data.accuracyRate").value(100.0)
			);
	}

	@Test
	@DisplayName("개인 정답률 조회 API 통합 테스트 - 학습모드 COMPLETE 세션 포함")
	void getPersonalAccuracy_StudyModeComplete() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();

		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("테스트 팀").creatorId(currentUser.getId()).build());

		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(currentUser, team));

		QuestionSetEntity studyQuestionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("학습 문제집")
				.teamId(team.getId())
				.deliveryMode(DeliveryMode.STUDY)
				.ongoingStatus(QuestionSetOngoingStatus.BEFORE)
				.build());

		SolvingSessionEntity session = SolvingSessionEntity.studySession(currentUser, studyQuestionSet);
		session.submit(2, 1);
		solvingSessionEntityRepository.save(session);

		MultipleQuestionEntity q1 = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(studyQuestionSet).content("문제 1").number(1L).lexoRank("a").build());

		MultipleQuestionEntity q2 = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(studyQuestionSet).content("문제 2").number(2L).lexoRank("b").build());

		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(currentUser.getId()).questionId(q1.getId())
			.submitOrder(1L).isCorrect(true).submittedAnswer("{}").build());

		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(currentUser.getId()).questionId(q2.getId())
			.submitOrder(2L).isCorrect(false).submittedAnswer("{}").build());

		// when & then
		mockMvc.perform(get("/api/v1/teams/{teamId}/user-solving-stats", team.getId())
				.accept(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.totalSolvedCount").value(2),
				jsonPath("$.data.correctCount").value(1),
				jsonPath("$.data.accuracyRate").value(50.0)
			);
	}

	@Test
	@DisplayName("개인 정답률 조회 API 통합 테스트 - 실시간 + 학습모드 합산")
	void getPersonalAccuracy_CombineLiveAndStudy() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();

		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("테스트 팀").creatorId(currentUser.getId()).build());

		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(currentUser, team));

		// 실시간 문제셋 (AFTER)
		QuestionSetEntity liveQuestionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("실시간 문제집")
				.teamId(team.getId())
				.deliveryMode(DeliveryMode.LIVE_TIME)
				.ongoingStatus(QuestionSetOngoingStatus.AFTER)
				.build());

		MultipleQuestionEntity liveQ = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(liveQuestionSet).content("실시간 문제").number(1L).lexoRank("a").build());

		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(currentUser.getId()).questionId(liveQ.getId())
			.submitOrder(1L).isCorrect(true).submittedAnswer("{}").build());

		// 학습모드 문제셋 (COMPLETE 세션)
		QuestionSetEntity studyQuestionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("학습 문제집")
				.teamId(team.getId())
				.deliveryMode(DeliveryMode.STUDY)
				.ongoingStatus(QuestionSetOngoingStatus.BEFORE)
				.build());

		SolvingSessionEntity session = SolvingSessionEntity.studySession(currentUser, studyQuestionSet);
		session.submit(1, 0);
		solvingSessionEntityRepository.save(session);

		MultipleQuestionEntity studyQ = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(studyQuestionSet).content("학습 문제").number(1L).lexoRank("a").build());

		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(currentUser.getId()).questionId(studyQ.getId())
			.submitOrder(2L).isCorrect(false).submittedAnswer("{}").build());

		// when & then: 총 2문제 중 1개 정답 = 50.0%
		mockMvc.perform(get("/api/v1/teams/{teamId}/user-solving-stats", team.getId())
				.accept(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.totalSolvedCount").value(2),
				jsonPath("$.data.correctCount").value(1),
				jsonPath("$.data.accuracyRate").value(50.0)
			);
	}

	@Test
	@DisplayName("개인 정답률 조회 API 통합 테스트 - 완료된 퀴즈가 없는 경우")
	void getPersonalAccuracy_NoCompletedQuiz() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();

		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("테스트 팀").creatorId(currentUser.getId()).build());

		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(currentUser, team));

		// when & then
		mockMvc.perform(get("/api/v1/teams/{teamId}/user-solving-stats", team.getId())
				.accept(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.totalSolvedCount").value(0),
				jsonPath("$.data.correctCount").value(0),
				jsonPath("$.data.accuracyRate").value(0.0)
			);
	}

	@Test
	@DisplayName("팀 정답 랭킹 조회 API 통합 테스트 - containsUserRank 필드 반환")
	void getTeamQuestionCorrectRank_ContainsUserRank() throws Exception {
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();

		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("랭킹 테스트 팀").creatorId(currentUser.getId()).build());
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(currentUser, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("랭킹 문제집")
				.teamId(team.getId())
				.deliveryMode(DeliveryMode.LIVE_TIME)
				.ongoingStatus(QuestionSetOngoingStatus.AFTER)
				.build());

		questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet).content("문제 1").number(1L).lexoRank("a").build());

		mockMvc.perform(get("/api/v1/teams/{teamId}/question-ranks", team.getId())
				.param("type", "CORRECT")
				.param("rankCount", "1")
				.accept(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.containsUserRank").value(true));
	}
}
