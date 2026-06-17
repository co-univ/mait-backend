package com.coniv.mait.web.admin.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

@WithCustomUser
public class AdminLookupApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@BeforeEach
	void clear() {
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
		teamEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("삭제되지 않은 모든 팀 목록을 조회한다")
	void getAllTeams_success() throws Exception {
		// given
		teamEntityRepository.save(TeamEntity.ofGroup("팀A", 1L));
		teamEntityRepository.save(TeamEntity.ofPersonal("개인", 2L));

		// when & then
		mockMvc.perform(get("/api/v1/admin/teams"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(2),
				jsonPath("$.data[*].name", hasItems("팀A", "개인")));
	}

	@Test
	@DisplayName("특정 팀의 문제 셋만 조회한다")
	void getQuestionSets_success() throws Exception {
		// given
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("팀A", 1L));
		TeamEntity otherTeam = teamEntityRepository.save(TeamEntity.ofGroup("팀B", 1L));

		questionSetEntityRepository.save(questionSet(team.getId(), "셋1"));
		questionSetEntityRepository.save(questionSet(team.getId(), "셋2"));
		questionSetEntityRepository.save(questionSet(otherTeam.getId(), "다른팀셋"));

		// when & then
		mockMvc.perform(get("/api/v1/admin/teams/{teamId}/question-sets", team.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(2),
				jsonPath("$.data[*].title", hasItems("셋1", "셋2")),
				jsonPath("$.data[*].teamId", everyItem(is(team.getId().intValue()))));
	}

	@Test
	@DisplayName("특정 문제 셋의 문제 목록을 실제 정답과 함께 조회한다")
	void getQuestions_success() throws Exception {
		// given
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("팀A", 1L));
		QuestionSetEntity questionSet = questionSetEntityRepository.save(questionSet(team.getId(), "셋1"));

		MultipleQuestionEntity question = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet)
				.content("문제 내용")
				.explanation("해설")
				.number(1L)
				.lexoRank("a")
				.build());
		multipleChoiceEntityRepository.saveAll(java.util.List.of(
			MultipleChoiceEntity.builder().number(1).content("정답").isCorrect(true).question(question).build(),
			MultipleChoiceEntity.builder().number(2).content("오답").isCorrect(false).question(question).build()));

		// when & then
		mockMvc.perform(get("/api/v1/admin/question-sets/{questionSetId}/questions", questionSet.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(1),
				jsonPath("$.data[0].id").value(question.getId()),
				jsonPath("$.data[0].type").value("MULTIPLE"),
				jsonPath("$.data[0].content").value("문제 내용"),
				jsonPath("$.data[0].choices.length()").value(2),
				jsonPath("$.data[0].choices[*].isCorrect", hasItem(true)));
	}

	private QuestionSetEntity questionSet(final Long teamId, final String title) {
		return QuestionSetEntity.builder()
			.teamId(teamId)
			.title(title)
			.status(QuestionSetStatus.AFTER)
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.build();
	}
}
