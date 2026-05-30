package com.coniv.mait.web.statistic.controller;

import static org.hamcrest.Matchers.*;
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
import com.coniv.mait.domain.user.enums.LoginProvider;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

@WithCustomUser
public class QuestionStatisticApiIntegrationTest extends BaseIntegrationTest {

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
	@DisplayName("문제별 최초 제출 기준 오답률을 계산해 높은 순으로 정렬하고, 제출이 없는 문제는 null로 맨 뒤에 조회한다")
	void getWrongRates_Success() throws Exception {
		// given
		UserEntity requester = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("wrongRateTeam", requester.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(requester, team));

		UserEntity userA = saveUser("a@example.com", "A");
		UserEntity userB = saveUser("b@example.com", "B");
		UserEntity userC = saveUser("c@example.com", "C");

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

		// q1: userA 최초 오답(후 재시도 정답), userB 정답 → 제출 2명 / 최초 오답 1명 → 50.0
		saveRecord(userA.getId(), q1.getId(), false, 1L);
		saveRecord(userA.getId(), q1.getId(), true, 4L);
		saveRecord(userB.getId(), q1.getId(), true, 2L);
		// q2: userA·userB·userC 모두 최초 오답 → 제출 3명 / 최초 오답 3명 → 100.0
		saveRecord(userA.getId(), q2.getId(), false, 3L);
		saveRecord(userB.getId(), q2.getId(), false, 5L);
		saveRecord(userC.getId(), q2.getId(), false, 6L);
		// q3: 제출 없음 → null

		// when & then
		mockMvc.perform(
				get("/api/v1/question-sets/{questionSetId}/questions/wrong-rates", questionSet.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(3),
				jsonPath("$.data[0].questionId").value(q2.getId()),
				jsonPath("$.data[0].questionNumber").value(2),
				jsonPath("$.data[0].submittedUserCount").value(3),
				jsonPath("$.data[0].firstWrongUserCount").value(3),
				jsonPath("$.data[0].wrongRate").value(100.0),
				jsonPath("$.data[1].questionId").value(q1.getId()),
				jsonPath("$.data[1].submittedUserCount").value(2),
				jsonPath("$.data[1].firstWrongUserCount").value(1),
				jsonPath("$.data[1].wrongRate").value(50.0),
				jsonPath("$.data[2].questionId").value(q3.getId()),
				jsonPath("$.data[2].submittedUserCount").value(0),
				jsonPath("$.data[2].firstWrongUserCount").value(0),
				jsonPath("$.data[2].wrongRate").value(nullValue()));
	}

	private UserEntity saveUser(final String email, final String name) {
		return userEntityRepository.save(
			UserEntity.socialLoginUser(email, name, "providerId-" + email, LoginProvider.GOOGLE));
	}

	private void saveRecord(final Long userId, final Long questionId, final boolean isCorrect, final Long submitOrder) {
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(userId)
			.questionId(questionId)
			.isCorrect(isCorrect)
			.submitOrder(submitOrder)
			.submittedAnswer("{}")
			.build());
	}
}
