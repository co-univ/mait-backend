package com.coniv.mait.web.solve.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
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

	@BeforeEach
	void clear() {
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
		final Long teamId = 1L;
		UserEntity user = userEntityRepository.save(
			UserEntity.localLoginUser("email", "testUser", "youth", "singsing"));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.teamId(teamId)
				.build()
		);

		MultipleQuestionEntity multipleQuestion = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.number(1L)
				.questionSet(questionSet)
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
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}/scorer",
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
}
