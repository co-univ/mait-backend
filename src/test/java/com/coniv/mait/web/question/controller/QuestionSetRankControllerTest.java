package com.coniv.mait.web.question.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.dto.AnswerRankDto;
import com.coniv.mait.domain.question.service.QuestionRankService;
import com.coniv.mait.domain.user.service.dto.UserDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

@WebMvcTest(controllers = QuestionSetRankController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionSetRankControllerTest {

	@MockitoBean
	private QuestionRankService questionRankService;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);
	}

	@Test
	@DisplayName("정답 개수에 따른 등수 조회 API 성공 테스트")
	void getCorrectorsByQuestionSetId_Success() throws Exception {
		// given
		final Long questionSetId = 1L;

		UserDto user1 = UserDto.builder()
			.id(1L)
			.email("user1@test.com")
			.name("사용자1")
			.nickname("닉네임1")
			.nicknameCode("001")
			.fullNickname("닉네임1#001")
			.build();

		UserDto user2 = UserDto.builder()
			.id(2L)
			.email("user2@test.com")
			.name("사용자2")
			.nickname("닉네임2")
			.nicknameCode("002")
			.fullNickname("닉네임2#002")
			.build();

		UserDto user3 = UserDto.builder()
			.id(3L)
			.email("user3@test.com")
			.name("사용자3")
			.nickname("닉네임3")
			.nicknameCode("003")
			.fullNickname("닉네임3#003")
			.build();

		AnswerRankDto rank1 = AnswerRankDto.builder()
			.count(1L)
			.users(List.of(user2))
			.build();

		AnswerRankDto rank2 = AnswerRankDto.builder()
			.count(2L)
			.users(List.of(user3))
			.build();

		AnswerRankDto rank3 = AnswerRankDto.builder()
			.count(3L)
			.users(List.of(user1))
			.build();

		List<AnswerRankDto> answerRanks = List.of(rank1, rank2, rank3);

		when(questionRankService.getCorrectorsByQuestionSetId(questionSetId)).thenReturn(answerRanks);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/ranks", questionSetId)
				.param("type", "CORRECT"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.questionSetId").value(questionSetId))
			.andExpect(jsonPath("$.data.ranksGroup").isArray())
			.andExpect(jsonPath("$.data.ranksGroup.length()").value(3))
			.andExpect(jsonPath("$.data.ranksGroup[0].answerCount").value(1L))
			.andExpect(jsonPath("$.data.ranksGroup[0].users.length()").value(1))
			.andExpect(jsonPath("$.data.ranksGroup[0].users[0].userId").value(2L))
			.andExpect(jsonPath("$.data.ranksGroup[0].users[0].name").value("사용자2"))
			.andExpect(jsonPath("$.data.ranksGroup[0].users[0].nickName").value("닉네임2"))
			.andExpect(jsonPath("$.data.ranksGroup[1].answerCount").value(2L))
			.andExpect(jsonPath("$.data.ranksGroup[1].users.length()").value(1))
			.andExpect(jsonPath("$.data.ranksGroup[1].users[0].userId").value(3L))
			.andExpect(jsonPath("$.data.ranksGroup[2].answerCount").value(3L))
			.andExpect(jsonPath("$.data.ranksGroup[2].users.length()").value(1))
			.andExpect(jsonPath("$.data.ranksGroup[2].users[0].userId").value(1L));

		verify(questionRankService).getCorrectorsByQuestionSetId(questionSetId);
	}
}
