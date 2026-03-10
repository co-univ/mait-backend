package com.coniv.mait.web.solve.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftEntity;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.solve.repository.StudyAnswerDraftEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

@WithCustomUser
public class StudyModeApiIntegrationTest extends BaseIntegrationTest {

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
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	@Autowired
	private StudyAnswerDraftEntityRepository studyAnswerDraftEntityRepository;

	@BeforeEach
	void clear() {
		solvingSessionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("학습모드 풀이 시작 시 세션과 문제별 draft가 생성된다")
	void startStudyMode_Success() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("testTeam").creatorId(user.getId()).build());
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.teamId(team.getId())
				.build());

		MultipleQuestionEntity q1 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(1L).lexoRank("a").build());
		MultipleQuestionEntity q2 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(2L).lexoRank("b").build());
		MultipleQuestionEntity q3 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(3L).lexoRank("c").build());

		// when & then
		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/study-mode", questionSet.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.solvingSessionId").exists(),
				jsonPath("$.data.questionSetId").value(questionSet.getId()),
				jsonPath("$.data.status").value("PROGRESSING"),
				jsonPath("$.data.startedAt").exists());

		List<StudyAnswerDraftEntity> drafts = studyAnswerDraftEntityRepository.findAll();
		assertThat(drafts).hasSize(3);
		assertThat(drafts).allSatisfy(draft -> assertThat(draft.getSubmittedAnswer()).isNull());
		assertThat(drafts).extracting(d -> d.getId().getQuestionId())
			.containsExactlyInAnyOrder(q1.getId(), q2.getId(), q3.getId());
	}

	@Test
	@DisplayName("이미 세션이 존재하면 draft를 중복 생성하지 않는다")
	void startStudyMode_ExistingSession_NoDuplicateDrafts() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("testTeam2").creatorId(user.getId()).build());
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.teamId(team.getId())
				.build());

		questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(1L).lexoRank("a").build());

		// when - 두 번 호출
		mockMvc.perform(
			post("/api/v1/question-sets/{questionSetId}/study-mode", questionSet.getId()))
			.andExpect(status().isOk());

		mockMvc.perform(
			post("/api/v1/question-sets/{questionSetId}/study-mode", questionSet.getId()))
			.andExpect(status().isOk());

		// then - draft는 1개만 존재
		assertThat(studyAnswerDraftEntityRepository.findAll()).hasSize(1);
		assertThat(solvingSessionEntityRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("학습모드 초안 조회 API 호출 시 세션의 draft 목록이 반환된다")
	void getStudyModeDrafts_Success() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("testTeam3").creatorId(user.getId()).build());
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.teamId(team.getId())
				.build());

		MultipleQuestionEntity q1 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(1L).lexoRank("a").build());
		MultipleQuestionEntity q2 = questionEntityRepository.save(
			MultipleQuestionEntity.builder().questionSet(questionSet).number(2L).lexoRank("b").build());

		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/study-mode", questionSet.getId()))
			.andExpect(status().isOk());

		// when & then
		mockMvc.perform(
				get("/api/v1/question-sets/{questionSetId}/study-mode/drafts", questionSet.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data[0].solvingSessionId").exists(),
				jsonPath("$.data[0].questionId").value(q1.getId()),
				jsonPath("$.data[0].submitted").value(false),
				jsonPath("$.data[1].questionId").value(q2.getId()),
				jsonPath("$.data[1].submitted").value(false));
	}
}
