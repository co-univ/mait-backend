package com.coniv.mait.web.statistic.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
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
public class TeamQuestionSetStatisticApiIntegrationTest extends BaseIntegrationTest {

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
	private QuestionSetParticipantRepository questionSetParticipantRepository;

	@Autowired
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@BeforeEach
	void clear() {
		answerSubmitRecordEntityRepository.deleteAll();
		questionSetParticipantRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("완료된 실시간 문제 셋의 통계(우승자/내 정답률/평균 정답률)를 조회한다")
	void getTeamQuestionSetStatistics_live_Success() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("statisticTeam", user.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.teamId(team.getId())
				.title("실시간 통계")
				.solveMode(QuestionSetSolveMode.LIVE_TIME)
				.status(QuestionSetStatus.AFTER)
				.endTime(LocalDateTime.of(2026, 5, 31, 12, 0))
				.build());

		MultipleQuestionEntity q1 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(1L).lexoRank("a").build());
		MultipleQuestionEntity q2 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(2L).lexoRank("b").build());

		// 우승자(=본인) 참여 기록
		questionSetParticipantRepository.save(QuestionSetParticipantEntity.builder()
			.questionSet(questionSet).user(user).winner(true).build());

		// q1 첫 제출 정답, q2 첫 제출 오답 → 정답 1 / 전체 2 = 50.0 (내 정답률 = 평균 정답률, 응시자 1명)
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(user.getId()).questionId(q1.getId()).isCorrect(true).submitOrder(1L).submittedAnswer("{\"a\":1}")
			.build());
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(user.getId()).questionId(q2.getId()).isCorrect(false).submitOrder(1L).submittedAnswer("{\"a\":2}")
			.build());

		// when & then
		mockMvc.perform(
				get("/api/v1/question-sets/statistics").param("teamId", String.valueOf(team.getId())))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(1),
				jsonPath("$.data[0].questionSetId").value(questionSet.getId()),
				jsonPath("$.data[0].title").value("실시간 통계"),
				jsonPath("$.data[0].solveMode").value("LIVE_TIME"),
				jsonPath("$.data[0].winners.length()").value(1),
				jsonPath("$.data[0].winners[0].userId").value(user.getId()),
				jsonPath("$.data[0].winners[0].name").value(user.getName()),
				jsonPath("$.data[0].winners[0].nickname").value(user.getNickname()),
				jsonPath("$.data[0].myCorrectRate").value(50.0),
				jsonPath("$.data[0].averageCorrectRate").value(50.0));
	}
}
