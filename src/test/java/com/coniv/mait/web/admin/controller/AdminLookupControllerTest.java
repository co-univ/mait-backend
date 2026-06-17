package com.coniv.mait.web.admin.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.admin.service.AdminLookupService;
import com.coniv.mait.domain.admin.service.dto.AdminQuestionSetDto;
import com.coniv.mait.domain.admin.service.dto.AdminTeamDto;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.QuestionDto;
import com.coniv.mait.domain.team.enums.TeamType;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

@WebMvcTest(controllers = AdminLookupController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminLookupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AdminLookupService adminLookupService;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@Test
	@DisplayName("모든 팀 목록 조회 성공")
	void getAllTeams_success() throws Exception {
		// given
		given(adminLookupService.getAllTeams()).willReturn(List.of(
			AdminTeamDto.builder()
				.teamId(1L).name("팀A").type(TeamType.GROUP).creatorId(100L).createdAt(LocalDateTime.now())
				.build()));

		// when & then
		mockMvc.perform(get("/api/v1/admin/teams"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(1),
				jsonPath("$.data[0].teamId").value(1),
				jsonPath("$.data[0].name").value("팀A"),
				jsonPath("$.data[0].type").value("GROUP"));
	}

	@Test
	@DisplayName("특정 팀의 문제 셋 목록 조회 성공")
	void getQuestionSets_success() throws Exception {
		// given
		Long teamId = 10L;
		given(adminLookupService.getQuestionSets(teamId)).willReturn(List.of(
			AdminQuestionSetDto.builder()
				.id(5L).title("셋1").subject("주제1")
				.status(QuestionSetStatus.AFTER).solveMode(QuestionSetSolveMode.LIVE_TIME).teamId(teamId)
				.createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
				.build()));

		// when & then
		mockMvc.perform(get("/api/v1/admin/teams/{teamId}/question-sets", teamId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(1),
				jsonPath("$.data[0].id").value(5),
				jsonPath("$.data[0].title").value("셋1"),
				jsonPath("$.data[0].status").value("AFTER"),
				jsonPath("$.data[0].teamId").value(teamId));
	}

	@Test
	@DisplayName("특정 문제 셋의 문제 목록 조회 성공")
	void getQuestions_success() throws Exception {
		// given
		Long questionSetId = 100L;
		List<QuestionDto> questions = List.of(
			MultipleQuestionDto.builder().id(7L).content("문제").explanation("해설").choices(List.of()).build());
		given(adminLookupService.getQuestions(questionSetId)).willReturn(questions);

		// when & then
		mockMvc.perform(get("/api/v1/admin/question-sets/{questionSetId}/questions", questionSetId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.length()").value(1),
				jsonPath("$.data[0].id").value(7),
				jsonPath("$.data[0].type").value("MULTIPLE"));
	}
}
