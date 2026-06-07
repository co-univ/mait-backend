package com.coniv.mait.web.statistic.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryLinkEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryLinkEntityRepository;
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
public class CategoryStatisticApiIntegrationTest extends BaseIntegrationTest {

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
	private QuestionSetCategoryEntityRepository questionSetCategoryEntityRepository;

	@Autowired
	private QuestionSetCategoryLinkEntityRepository questionSetCategoryLinkEntityRepository;

	@Autowired
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@BeforeEach
	void clear() {
		answerSubmitRecordEntityRepository.deleteAll();
		questionSetCategoryLinkEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
		questionSetCategoryEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("팀 카테고리별 내 정답률을 높은 순으로 조회하고, 종료 셋이 없는 카테고리는 정답률 null로 맨 뒤에 둔다")
	void getCategoryCorrectRates_Success() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("categoryStatTeam", user.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user, team));

		QuestionSetCategoryEntity javaCategory =
			questionSetCategoryEntityRepository.save(QuestionSetCategoryEntity.of(team.getId(), "Java"));
		QuestionSetCategoryEntity springCategory =
			questionSetCategoryEntityRepository.save(QuestionSetCategoryEntity.of(team.getId(), "Spring"));
		QuestionSetCategoryEntity dbCategory =
			questionSetCategoryEntityRepository.save(QuestionSetCategoryEntity.of(team.getId(), "DB"));

		// Java: 종료 셋, 두 문제 모두 정답 → 내 정답률 100.0
		QuestionSetEntity javaSet = saveQuestionSet(team.getId(), "자바", QuestionSetStatus.AFTER);
		MultipleQuestionEntity jq1 = saveQuestion(javaSet, 1L, "a");
		MultipleQuestionEntity jq2 = saveQuestion(javaSet, 2L, "b");
		saveSubmit(user.getId(), jq1.getId(), true);
		saveSubmit(user.getId(), jq2.getId(), true);
		link(javaSet.getId(), javaCategory.getId());

		// Spring: 종료 셋, 한 문제만 정답 → 내 정답률 50.0
		QuestionSetEntity springSet = saveQuestionSet(team.getId(), "스프링", QuestionSetStatus.AFTER);
		MultipleQuestionEntity sq1 = saveQuestion(springSet, 1L, "a");
		MultipleQuestionEntity sq2 = saveQuestion(springSet, 2L, "b");
		saveSubmit(user.getId(), sq1.getId(), true);
		saveSubmit(user.getId(), sq2.getId(), false);
		link(springSet.getId(), springCategory.getId());

		// DB: 진행 중 셋만 연결 → 종료 셋 없음
		QuestionSetEntity ongoingSet = saveQuestionSet(team.getId(), "디비", QuestionSetStatus.ONGOING);
		link(ongoingSet.getId(), dbCategory.getId());

		// when & then
		mockMvc.perform(
				get("/api/v1/teams/{teamId}/categories/correct-rates", team.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(3),
				jsonPath("$.data[0].categoryName").value("Java"),
				jsonPath("$.data[0].questionSetCount").value(1),
				jsonPath("$.data[0].myCorrectRate").value(100.0),
				jsonPath("$.data[0].averageCorrectRate").value(100.0),
				jsonPath("$.data[1].categoryName").value("Spring"),
				jsonPath("$.data[1].questionSetCount").value(1),
				jsonPath("$.data[1].myCorrectRate").value(50.0),
				jsonPath("$.data[2].categoryName").value("DB"),
				jsonPath("$.data[2].questionSetCount").value(0),
				jsonPath("$.data[2].myCorrectRate").doesNotExist(),
				jsonPath("$.data[2].averageCorrectRate").value(0.0));
	}

	private QuestionSetEntity saveQuestionSet(final Long teamId, final String title, final QuestionSetStatus status) {
		return questionSetEntityRepository.save(QuestionSetEntity.builder()
			.teamId(teamId)
			.title(title)
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(status)
			.endTime(LocalDateTime.of(2026, 5, 31, 12, 0))
			.build());
	}

	private MultipleQuestionEntity saveQuestion(final QuestionSetEntity questionSet, final Long number,
		final String lexoRank) {
		return questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(number).lexoRank(lexoRank).build());
	}

	private void saveSubmit(final Long userId, final Long questionId, final boolean isCorrect) {
		answerSubmitRecordEntityRepository.save(AnswerSubmitRecordEntity.builder()
			.userId(userId).questionId(questionId).isCorrect(isCorrect).submitOrder(1L).submittedAnswer("{\"a\":1}")
			.build());
	}

	private void link(final Long questionSetId, final Long categoryId) {
		questionSetCategoryLinkEntityRepository.save(QuestionSetCategoryLinkEntity.of(questionSetId, categoryId));
	}
}
