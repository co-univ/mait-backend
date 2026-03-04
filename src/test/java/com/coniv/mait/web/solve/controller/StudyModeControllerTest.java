package com.coniv.mait.web.solve.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.service.StudyModeService;
import com.coniv.mait.domain.solve.service.dto.SolvingSessionDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

@WebMvcTest(controllers = StudyModeController.class)
@AutoConfigureMockMvc(addFilters = false)
class StudyModeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StudyModeService studyModeService;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@Test
	@DisplayName("학습모드 풀이 시작 성공")
	void startStudySubmission_Success() throws Exception {
		// given
		Long questionSetId = 1L;
		LocalDateTime startedAt = LocalDateTime.of(2026, 3, 4, 10, 0);

		SolvingSessionDto session = SolvingSessionDto.builder()
			.id(100L)
			.questionSetId(questionSetId)
			.userId(1L)
			.status(SolvingStatus.PROGRESSING)
			.mode(DeliveryMode.STUDY)
			.startedAt(startedAt)
			.build();

		given(studyModeService.startStudyMode(any(), eq(questionSetId))).willReturn(session);

		// when & then
		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/study-mode", questionSetId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.solvingSessionId").value(100),
				jsonPath("$.data.questionSetId").value(questionSetId),
				jsonPath("$.data.status").value("PROGRESSING"));

		verify(studyModeService).startStudyMode(any(), eq(questionSetId));
	}
}
