package com.coniv.mait.web.question.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.service.QuestionReviewService;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.question.dto.LastViewedQuestionApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

class QuestionReviewApiIntegrationTest extends BaseIntegrationTest {

	private static final Long USER_ID = 2L;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private QuestionReviewService questionReviewService;

	private Authentication authentication;

	@BeforeEach
	void setUp() {
		UserEntity user = mock(UserEntity.class);
		when(user.getId()).thenReturn(USER_ID);
		authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());

		// addFilters=false 환경에서 @AuthenticationPrincipal을 안전하게 주입하기 위해 SecurityContext를 직접 세팅
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("마지막 조회한 문제 조회 API - 성공")
	void getLastViewedQuestion_success() throws Exception {
		// given
		final Long questionSetId = 1L;

		MultipleQuestionDto questionDto = MultipleQuestionDto.builder()
			.id(10L)
			.content("객관식 문제 내용")
			.explanation("문제 해설")
			.number(1L)
			.choices(List.of(
				MultipleChoiceDto.builder().id(100L).number(1).content("선택지1").isCorrect(true).build(),
				MultipleChoiceDto.builder().id(101L).number(2).content("선택지2").isCorrect(false).build()
			))
			.build();

		when(questionReviewService.getLastViewedQuestionInReview(questionSetId, USER_ID)).thenReturn(questionDto);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/last-viewed", questionSetId)
				.with(authentication(authentication)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").exists(),
				jsonPath("$.data.id").value(10L),
				jsonPath("$.data.type").value("MULTIPLE"),
				jsonPath("$.data.content").value("객관식 문제 내용")
			);

		verify(questionReviewService).getLastViewedQuestionInReview(questionSetId, USER_ID);
	}

	@Test
	@DisplayName("마지막 조회한 문제 업데이트 API - 성공")
	void updateLastViewedQuestion_success() throws Exception {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 10L;
		LastViewedQuestionApiRequest request = new LastViewedQuestionApiRequest(questionId);
		String json = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/last-viewed", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json)
				.with(authentication(authentication)))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").doesNotExist()
			);

		verify(questionReviewService).updateLastViewedQuestion(questionSetId, questionId, USER_ID);
	}
}


