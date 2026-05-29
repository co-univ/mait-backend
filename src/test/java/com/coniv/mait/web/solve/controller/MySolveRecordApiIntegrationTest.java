package com.coniv.mait.web.solve.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

@WithCustomUser
public class MySolveRecordApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@BeforeEach
	void clear() {
		answerSubmitRecordEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("학습모드 채점 기록(문제당 1건)으로 풀이 기록과 100점 만점 점수를 조회한다")
	void getMySolveRecord_study_Success() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("studyRecordTeam", user.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.teamId(team.getId())
				.solveMode(QuestionSetSolveMode.STUDY)
				.status(QuestionSetStatus.AFTER)
				.build());

		MultipleQuestionEntity q1 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(1L).lexoRank("a").build());
		MultipleQuestionEntity q2 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(2L).lexoRank("b").build());

		// 학습모드는 채점 시 전 문제에 기록이 1건씩 생성된다 (미제출은 오답 기록)
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(user.getId()).questionId(q1.getId()).isCorrect(true).submittedAnswer("{\"a\":1}").build());
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(user.getId()).questionId(q2.getId()).isCorrect(false).submittedAnswer(null).build());

		// when & then
		mockMvc.perform(
				get("/api/v1/question-sets/{questionSetId}/user/result", questionSet.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.questionSetId").value(questionSet.getId()),
				jsonPath("$.data.solveMode").value("STUDY"),
				jsonPath("$.data.totalCount").value(2),
				jsonPath("$.data.correctCount").value(1),
				jsonPath("$.data.score").value(50.0),
				jsonPath("$.data.results.length()").value(2),
				jsonPath("$.data.results[0].questionId").value(q1.getId()),
				jsonPath("$.data.results[0].isCorrect").value(true),
				jsonPath("$.data.results[1].questionId").value(q2.getId()),
				jsonPath("$.data.results[1].isCorrect").value(false));
	}

	@Test
	@DisplayName("실시간모드는 최초 제출 기준으로 채점하고 미응답 문제도 결과에 포함해 조회한다")
	void getMySolveRecord_live_Success() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("liveRecordTeam", user.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.teamId(team.getId())
				.solveMode(QuestionSetSolveMode.LIVE_TIME)
				.status(QuestionSetStatus.AFTER)
				.build());

		MultipleQuestionEntity q1 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(1L).lexoRank("a").build());
		MultipleQuestionEntity q2 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(2L).lexoRank("b").build());
		MultipleQuestionEntity q3 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(3L).lexoRank("c").build());

		// q1: 첫 제출에 정답 → 정답 집계. q2: 오답 후 정답(재시도) → 최초 제출(오답)이 대표 → 오답 집계.
		// q3: 미응답 → 분모에만 포함. 정답 1 / 전체 3 = 33.3점
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(user.getId()).questionId(q1.getId()).isCorrect(true).submitOrder(1L).submittedAnswer("{\"a\":1}")
			.build());
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(user.getId()).questionId(q2.getId()).isCorrect(false).submitOrder(2L).submittedAnswer("{\"a\":2}")
			.build());
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(user.getId()).questionId(q2.getId()).isCorrect(true).submitOrder(3L).submittedAnswer("{\"a\":3}")
			.build());

		// when & then
		mockMvc.perform(
				get("/api/v1/question-sets/{questionSetId}/user/result", questionSet.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.questionSetId").value(questionSet.getId()),
				jsonPath("$.data.solveMode").value("LIVE_TIME"),
				jsonPath("$.data.totalCount").value(3),
				jsonPath("$.data.correctCount").value(1),
				jsonPath("$.data.score").value(33.3),
				jsonPath("$.data.results.length()").value(3),
				jsonPath("$.data.results[0].questionId").value(q1.getId()),
				jsonPath("$.data.results[0].isCorrect").value(true),
				jsonPath("$.data.results[1].questionId").value(q2.getId()),
				jsonPath("$.data.results[1].isCorrect").value(false),
				jsonPath("$.data.results[2].questionId").value(q3.getId()),
				jsonPath("$.data.results[2].isCorrect").value(false),
				jsonPath("$.data.results[2].submittedAnswer").doesNotExist());
	}
}
