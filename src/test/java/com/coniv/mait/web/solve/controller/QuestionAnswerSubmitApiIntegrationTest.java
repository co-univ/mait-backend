package com.coniv.mait.web.solve.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.enums.QuestionStatusType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.service.dto.MultipleQuestionSubmitAnswer;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.solve.dto.MultipleQuestionSubmitApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class QuestionAnswerSubmitApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private QuestionSetParticipantRepository questionSetParticipantRepository;

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Autowired
	private ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Autowired
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Autowired
	private OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Autowired
	private QuestionScorerEntityRepository questionScorerEntityRepository;

	@Autowired
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@BeforeEach
	void clear() {
		answerSubmitRecordEntityRepository.deleteAll();
		questionScorerEntityRepository.deleteAll();
		multipleChoiceEntityRepository.deleteAll();
		fillBlankAnswerEntityRepository.deleteAll();
		shortAnswerEntityRepository.deleteAll();
		orderingOptionEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("객관식 문제 정답 제출 성공 API 테스트")
	void submitMultipleChoiceQuestionAnswer_Success() throws Exception {
		// Given
		UserEntity user = userEntityRepository.save(
			UserEntity.localLoginUser("email", "testUser", "youth", "singsing"));
		TeamEntity team = teamEntityRepository.save(TeamEntity.builder().name("coniv").creatorId(user.getId()).build());
		final Long teamId = team.getId();
		TeamUserEntity teamUser = teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.teamId(teamId)
				.build()
		);

		QuestionSetParticipantEntity questionSetParticipant = questionSetParticipantRepository.save(
			QuestionSetParticipantEntity.builder()
				.status(ParticipantStatus.ACTIVE)
				.questionSet(questionSet)
				.user(user)
				.winner(false)
				.build());

		MultipleQuestionEntity multipleQuestion = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.number(1L)
				.questionSet(questionSet)
				.questionStatus(QuestionStatusType.SOLVE_PERMISSION)
				.lexoRank("m")
				.build()
		);

		MultipleChoiceEntity multipleChoice1 = MultipleChoiceEntity.builder()
			.question(multipleQuestion)
			.number(1)
			.isCorrect(true)
			.build();

		MultipleChoiceEntity multipleChoice2 = MultipleChoiceEntity.builder()
			.question(multipleQuestion)
			.number(2)
			.isCorrect(false)
			.build();

		multipleChoiceEntityRepository.saveAll(List.of(multipleChoice1, multipleChoice2));

		// 요청 DTO 생성
		MultipleQuestionSubmitApiRequest request = MultipleQuestionSubmitApiRequest.builder()
			.userId(1L)
			.submitAnswers(List.of(1L))
			.build();

		ObjectNode node = objectMapper.createObjectNode();
		node.put("type", "MULTIPLE");
		node.put("userId", user.getId());
		node.set("submitAnswers", objectMapper.valueToTree(List.of(1L)));

		// When & Then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit",
				questionSet.getId(), multipleQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(node)))
			.andExpect(status().isOk())
			.andExpectAll(
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.id").exists(),
				jsonPath("$.data.userId").value(user.getId()),
				jsonPath("$.data.questionId").value(multipleQuestion.getId()),
				jsonPath("$.data.isCorrect").value(true)
			);
	}

	@Test
	@DisplayName("문제 셋이 PRIVATE인 경우 답안 제출 시 NEED_OPEN(FORBIDDEN) 반환")
	void submitAnswer_WhenQuestionSetPrivate_ReturnNeedOpen() throws Exception {
		// Given
		UserEntity user = userEntityRepository.save(
			UserEntity.localLoginUser("email", "testUser", "youth", "singsing"));
		TeamEntity team = teamEntityRepository.save(TeamEntity.builder().name("coniv").creatorId(user.getId()).build());
		final Long teamId = team.getId();
		teamUserEntityRepository.save(TeamUserEntity.createPlayerUser(user, team));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.teamId(teamId)
				.visibility(QuestionSetVisibility.PRIVATE)
				.build()
		);

		questionSetParticipantRepository.save(
			QuestionSetParticipantEntity.builder()
				.status(ParticipantStatus.ACTIVE)
				.questionSet(questionSet)
				.user(user)
				.winner(false)
				.build()
		);

		MultipleQuestionEntity multipleQuestion = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.number(1L)
				.questionSet(questionSet)
				.questionStatus(QuestionStatusType.SOLVE_PERMISSION)
				.lexoRank("m")
				.build()
		);

		ObjectNode node = objectMapper.createObjectNode();
		node.put("type", "MULTIPLE");
		node.put("userId", user.getId());
		node.set("submitAnswers", objectMapper.valueToTree(List.of(1L)));

		// When & Then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit",
				questionSet.getId(), multipleQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(node)))
			.andExpect(status().isForbidden())
			.andExpectAll(
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.status").value(HttpStatus.FORBIDDEN.name()),
				jsonPath("$.code").value("1001")
			);
	}

	@Test
	@DisplayName("문제별 득점자 조회 성공 API 테스트")
	void getScorer_Success() throws Exception {
		// Given
		final Long teamId = 1L;
		UserEntity user = userEntityRepository.save(
			UserEntity.localLoginUser("email", "testUser", "테스트사용자", "singsing"));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.teamId(teamId)
				.build()
		);

		MultipleQuestionEntity multipleQuestion = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.number(1L)
				.questionSet(questionSet)
				.lexoRank("m")
				.build()
		);

		MultipleChoiceEntity multipleChoice1 = MultipleChoiceEntity.builder()
			.question(multipleQuestion)
			.number(1)
			.isCorrect(true)
			.build();

		multipleChoiceEntityRepository.save(multipleChoice1);

		QuestionScorerEntity scorer = questionScorerEntityRepository.save(
			QuestionScorerEntity.builder()
				.questionId(multipleQuestion.getId())
				.userId(user.getId())
				.submitOrder(1L)
				.build()
		);

		// When & Then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}/scorers",
				questionSet.getId(), multipleQuestion.getId()))
			.andExpect(status().isOk())
			.andExpectAll(
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.id").value(scorer.getId()),
				jsonPath("$.data.questionId").value(multipleQuestion.getId()),
				jsonPath("$.data.userId").value(user.getId()),
				jsonPath("$.data.userName").value("테스트사용자"),
				jsonPath("$.data.submitOrder").value(1)
			);
	}

	@Test
	@DisplayName("문제 풀이 정답 제출 기록 조회 API 통합 테스트")
	void getSubmitRecords_IntegrationTest() throws Exception {
		// Given
		UserEntity user1 = userEntityRepository.save(UserEntity.localLoginUser(
			"user1@test.com", "password", "사용자1", "user1"));

		UserEntity user2 = userEntityRepository.save(UserEntity.localLoginUser(
			"user2@test.com", "password", "사용자2", "user2"));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(QuestionSetEntity.builder()
			.title("테스트 문제집")
			.subject("테스트 과목")
			.build());

		MultipleQuestionEntity multipleQuestion = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.content("이것은 테스트 문제입니다.")
				.number(1L)
				.questionSet(questionSet)
				.lexoRank("m")
				.answerCount(4)
				.build());

		// 정답 제출 기록 생성
		String correctSubmitAnswer = objectMapper.writeValueAsString(
			new MultipleQuestionSubmitAnswer(List.of(1L)));
		String incorrectSubmitAnswer = objectMapper.writeValueAsString(
			new MultipleQuestionSubmitAnswer(List.of(2L)));

		AnswerSubmitRecordEntity record1 = answerSubmitRecordEntityRepository.save(
			AnswerSubmitRecordEntity.builder()
				.userId(user1.getId())
				.questionId(multipleQuestion.getId())
				.submitOrder(1L)
				.isCorrect(true)
				.submittedAnswer(correctSubmitAnswer)
				.build()
		);

		AnswerSubmitRecordEntity record2 = answerSubmitRecordEntityRepository.save(
			AnswerSubmitRecordEntity.builder()
				.userId(user2.getId())
				.questionId(multipleQuestion.getId())
				.submitOrder(2L)
				.isCorrect(false)
				.submittedAnswer(incorrectSubmitAnswer)
				.build()
		);

		// When & Then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit-records",
				questionSet.getId(), multipleQuestion.getId()))
			.andExpect(status().isOk())
			.andExpectAll(
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.totalCounts").value(2),
				jsonPath("$.data.submitRecords[0].id").value(record1.getId()),
				jsonPath("$.data.submitRecords[0].userId").value(user1.getId()),
				jsonPath("$.data.submitRecords[0].userName").value("사용자1"),
				jsonPath("$.data.submitRecords[0].questionId").value(multipleQuestion.getId()),
				jsonPath("$.data.submitRecords[0].isCorrect").value(true),
				jsonPath("$.data.submitRecords[0].submitOrder").value(1),
				jsonPath("$.data.submitRecords[1].id").value(record2.getId()),
				jsonPath("$.data.submitRecords[1].userId").value(user2.getId()),
				jsonPath("$.data.submitRecords[1].userName").value("사용자2"),
				jsonPath("$.data.submitRecords[1].questionId").value(multipleQuestion.getId()),
				jsonPath("$.data.submitRecords[1].isCorrect").value(false),
				jsonPath("$.data.submitRecords[1].submitOrder").value(2)
			);
	}
}
