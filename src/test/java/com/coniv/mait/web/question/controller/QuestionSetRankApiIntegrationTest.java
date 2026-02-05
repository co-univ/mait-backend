package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
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
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
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
import com.coniv.mait.web.integration.BaseIntegrationTest;

@Import(TestRedisConfig.class)
public class QuestionSetRankApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private QuestionSetParticipantRepository questionSetParticipantRepository;

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

	@BeforeEach
	void setUp() {
		answerSubmitRecordEntityRepository.deleteAll();
		questionSetParticipantRepository.deleteAll();
		teamUserEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
		userEntityRepository.deleteAll();
		teamEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("정답 개수에 따른 등수 조회 API 통합 테스트")
	void getCorrectorsByQuestionSetId_IntegrationTest() throws Exception {
		// given
		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("테스트 팀").creatorId(1L).build());

		UserEntity user1 = userEntityRepository.save(
			UserEntity.localLoginUser("user1@test.com", "password", "사용자1", "user1"));
		UserEntity user2 = userEntityRepository.save(
			UserEntity.localLoginUser("user2@test.com", "password", "사용자2", "user2"));
		UserEntity user3 = userEntityRepository.save(
			UserEntity.localLoginUser("user3@test.com", "password", "사용자3", "user3"));

		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user1, team));
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user2, team));
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user3, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("테스트 문제집")
				.teamId(team.getId())
				.build());

		questionSetParticipantRepository.save(
			QuestionSetParticipantEntity.createActiveParticipant(questionSet, user1));
		questionSetParticipantRepository.save(
			QuestionSetParticipantEntity.createActiveParticipant(questionSet, user2));
		questionSetParticipantRepository.save(
			QuestionSetParticipantEntity.createActiveParticipant(questionSet, user3));

		MultipleQuestionEntity question1 = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet)
				.content("문제 1")
				.number(1L)
				.lexoRank("a")
				.build());

		MultipleQuestionEntity question2 = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet)
				.content("문제 2")
				.number(2L)
				.lexoRank("b")
				.build());

		MultipleQuestionEntity question3 = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet)
				.content("문제 3")
				.number(3L)
				.lexoRank("c")
				.build());

		answerSubmitRecordEntityRepository.save(
			AnswerSubmitRecordEntity.builder()
				.userId(user1.getId())
				.questionId(question1.getId())
				.submitOrder(1L)
				.isCorrect(true)
				.submittedAnswer("{}")
				.build());

		answerSubmitRecordEntityRepository.save(
			AnswerSubmitRecordEntity.builder()
				.userId(user1.getId())
				.questionId(question2.getId())
				.submitOrder(2L)
				.isCorrect(true)
				.submittedAnswer("{}")
				.build());

		answerSubmitRecordEntityRepository.save(
			AnswerSubmitRecordEntity.builder()
				.userId(user1.getId())
				.questionId(question3.getId())
				.submitOrder(3L)
				.isCorrect(true)
				.submittedAnswer("{}")
				.build());

		answerSubmitRecordEntityRepository.save(
			AnswerSubmitRecordEntity.builder()
				.userId(user2.getId())
				.questionId(question1.getId())
				.submitOrder(4L)
				.isCorrect(true)
				.submittedAnswer("{}")
				.build());

		answerSubmitRecordEntityRepository.save(
			AnswerSubmitRecordEntity.builder()
				.userId(user3.getId())
				.questionId(question1.getId())
				.submitOrder(5L)
				.isCorrect(true)
				.submittedAnswer("{}")
				.build());

		answerSubmitRecordEntityRepository.save(
			AnswerSubmitRecordEntity.builder()
				.userId(user3.getId())
				.questionId(question2.getId())
				.submitOrder(6L)
				.isCorrect(true)
				.submittedAnswer("{}")
				.build());

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/ranks", questionSet.getId())
				.param("type", "CORRECT")
				.accept(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.questionSetId").value(questionSet.getId()),
				jsonPath("$.data.ranksGroup").isArray(),
				jsonPath("$.data.ranksGroup.length()").value(3),
				jsonPath("$.data.ranksGroup[0].answerCount").value(3L),
				jsonPath("$.data.ranksGroup[0].users.length()").value(1),
				jsonPath("$.data.ranksGroup[0].users[0].userId").value(user1.getId()),
				jsonPath("$.data.ranksGroup[0].users[0].name").value("사용자1"),
				jsonPath("$.data.ranksGroup[1].answerCount").value(2L),
				jsonPath("$.data.ranksGroup[1].users.length()").value(1),
				jsonPath("$.data.ranksGroup[1].users[0].userId").value(user3.getId()),
				jsonPath("$.data.ranksGroup[1].users[0].name").value("사용자3"),
				jsonPath("$.data.ranksGroup[2].answerCount").value(1L),
				jsonPath("$.data.ranksGroup[2].users.length()").value(1),
				jsonPath("$.data.ranksGroup[2].users[0].userId").value(user2.getId()),
				jsonPath("$.data.ranksGroup[2].users[0].name").value("사용자2")
			);

		// then
		assertThat(answerSubmitRecordEntityRepository.count()).isEqualTo(6);
		assertThat(questionSetEntityRepository.count()).isEqualTo(1);
		assertThat(userEntityRepository.count()).isEqualTo(3);
	}
}
