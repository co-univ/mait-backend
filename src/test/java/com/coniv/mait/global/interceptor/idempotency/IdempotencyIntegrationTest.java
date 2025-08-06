package com.coniv.mait.global.interceptor.idempotency;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class IdempotencyIntegrationTest extends BaseIntegrationTest {

	@MockitoBean
	IdempotencyRedisRepository redisRepository;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@BeforeEach
	void setUp() {
		userEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("처음 요청이면 정상 처리 후 PROCESSING 저장")
	void firstRequest_processingStored() throws Exception {
		// given
		final String idempotencyKey = "abc123";
		given(redisRepository.getStatus(idempotencyKey)).willReturn(null);

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

		ObjectNode node = objectMapper.createObjectNode();
		node.put("type", "MULTIPLE");
		node.put("userId", user.getId());
		node.set("submitAnswers", objectMapper.valueToTree(List.of(1L)));

		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSet.getId(),
				multipleQuestion.getId())
				.header("Idempotency-Key", idempotencyKey)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(node)))
			.andExpect(status().isOk());

		verify(redisRepository).setProcessing(idempotencyKey);
	}

	@Test
	@DisplayName("처리중 상태면 409 반환")
	void processing_conflict() throws Exception {
		// given
		given(redisRepository.getStatus("abc123")).willReturn(IdempotencyStatus.PROCESSING);

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

		ObjectNode node = objectMapper.createObjectNode();
		node.put("type", "MULTIPLE");
		node.put("userId", user.getId());
		node.set("submitAnswers", objectMapper.valueToTree(List.of(1L)));

		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit", questionSet.getId(),
				multipleQuestion.getId())
				.header("Idempotency-Key", "abc123")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(node)))
			.andExpect(status().isConflict())
			.andExpectAll(
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.status").value(HttpStatus.CONFLICT.name())
			);
	}

	@Test
	@DisplayName("완료 상태면 캐시된 응답 반환")
	void completed_returnCached() throws Exception {
		given(redisRepository.getStatus("abc123")).willReturn(IdempotencyStatus.COMPLETED);
		given(redisRepository.getResponse("abc123")).willReturn(Map.of("result", "ok"));

		mockMvc.perform(post("/api/v1/question-sets/1/questions/1/submit")
				.header("Idempotency-Key", "abc123")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"userId\": 1, \"submitAnswers\": []}"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("\"result\":\"ok\"")));
	}
}
