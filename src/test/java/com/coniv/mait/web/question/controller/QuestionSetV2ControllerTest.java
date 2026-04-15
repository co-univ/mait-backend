package com.coniv.mait.web.question.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.service.QuestionSetViewQueryService;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.question.dto.QuestionSetGroupBy;
import com.coniv.mait.web.question.dto.QuestionSetView;
import com.coniv.mait.web.question.dto.QuestionSetViewApiResponse;
import com.coniv.mait.web.question.dto.QuestionSetViewItem;
import com.coniv.mait.web.question.dto.QuestionSetViewSection;
import com.coniv.mait.web.question.dto.QuestionSetViewType;
import com.coniv.mait.web.question.dto.UserParticipationStatus;

@WebMvcTest(controllers = QuestionSetV2Controller.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionSetV2ControllerTest {

	private static final Long USER_ID = 10L;

	@MockitoBean
	private QuestionSetViewQueryService questionSetViewQueryService;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		MaitUser user = MaitUser.builder().id(USER_ID).build();
		var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
		var context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("풀이 관점 문제 셋 목록 조회 V2 - type을 query parameter로 받는다")
	void getSolvingQuestionSets() throws Exception {
		// given
		Long teamId = 1L;
		QuestionSetViewItem item = QuestionSetViewItem.builder()
			.id(1L)
			.subject("실시간 문제")
			.userParticipationStatus(UserParticipationStatus.PARTICIPATING)
			.build();
		QuestionSetViewApiResponse response = QuestionSetViewApiResponse.of(
			QuestionSetView.SOLVING,
			QuestionSetViewType.LIVE_TIME,
			QuestionSetGroupBy.USER_PARTICIPATION_STATUS,
			List.of(QuestionSetViewSection.of("PARTICIPATING", "참가중", List.of(item))));

		when(questionSetViewQueryService.getLiveSolvingQuestionSets(
			eq(teamId),
			any(MaitUser.class))).thenReturn(response);

		// when & then
		mockMvc.perform(get("/api/v2/teams/{teamId}/question-sets/solving", teamId)
				.param("type", QuestionSetViewType.LIVE_TIME.name()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data.view").value("SOLVING"),
				jsonPath("$.data.type").value("LIVE_TIME"),
				jsonPath("$.data.groupBy").value("USER_PARTICIPATION_STATUS"),
				jsonPath("$.data.sections[0].key").value("PARTICIPATING"),
				jsonPath("$.data.sections[0].items[0].id").value(1L),
				jsonPath("$.data.sections[0].items[0].subject").value("실시간 문제"));

		verify(questionSetViewQueryService).getLiveSolvingQuestionSets(
			eq(teamId),
			any(MaitUser.class));
	}

	@Test
	@DisplayName("관리 관점 문제 셋 목록 조회 V2 - type을 query parameter로 받는다")
	void getManagementQuestionSets() throws Exception {
		// given
		Long teamId = 1L;
		QuestionSetViewApiResponse response = QuestionSetViewApiResponse.of(
			QuestionSetView.MANAGEMENT,
			QuestionSetViewType.MAKING,
			QuestionSetGroupBy.NONE,
			List.of(QuestionSetViewSection.of("ALL", "전체", List.of())));

		when(questionSetViewQueryService.getMakingManagementQuestionSets(
			eq(teamId),
			any(MaitUser.class))).thenReturn(response);

		// when & then
		mockMvc.perform(get("/api/v2/teams/{teamId}/question-sets/management", teamId)
				.param("type", QuestionSetViewType.MAKING.name()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data.view").value("MANAGEMENT"),
				jsonPath("$.data.type").value("MAKING"),
				jsonPath("$.data.groupBy").value("NONE"),
				jsonPath("$.data.sections[0].key").value("ALL"));

		verify(questionSetViewQueryService).getMakingManagementQuestionSets(
			eq(teamId),
			any(MaitUser.class));
	}

	@Test
	@DisplayName("풀이 관점 문제 셋 목록 조회 V2 - 제작중 type은 컨트롤러에서 차단한다")
	void getSolvingQuestionSets_WithMakingType_ReturnsBadRequest() throws Exception {
		// given
		Long teamId = 1L;

		// when & then
		mockMvc.perform(get("/api/v2/teams/{teamId}/question-sets/solving", teamId)
				.param("type", QuestionSetViewType.MAKING.name()))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.code").value("C-003"),
				jsonPath("$.reasons[0]").value("풀이 목록에서 지원하지 않는 type입니다: MAKING"));

		verifyNoInteractions(questionSetViewQueryService);
	}
}
