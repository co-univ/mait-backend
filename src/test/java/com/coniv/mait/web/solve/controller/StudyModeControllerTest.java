package com.coniv.mait.web.solve.controller;

import static org.mockito.ArgumentMatchers.*;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.service.StudyModeService;
import com.coniv.mait.domain.solve.service.dto.SolvingSessionDto;
import com.coniv.mait.domain.solve.service.dto.StudyAnswerDraftDto;
import com.coniv.mait.domain.solve.service.dto.SubmitAnswerDto;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;

import jakarta.persistence.EntityNotFoundException;

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

	@Test
	@DisplayName("학습모드 초안 조회 성공")
	void getStudyDrafts_Success() throws Exception {
		// given
		Long questionSetId = 1L;
		StudyAnswerDraftDto draft1 = StudyAnswerDraftDto.builder()
			.solvingSessionId(100L)
			.questionId(11L)
			.submittedAnswer("{\"type\":\"SHORT\",\"submitAnswers\":[\"A\"]}")
			.submitted(true)
			.build();
		StudyAnswerDraftDto draft2 = StudyAnswerDraftDto.builder()
			.solvingSessionId(100L)
			.questionId(12L)
			.submittedAnswer(null)
			.submitted(false)
			.build();

		given(studyModeService.getStudyAnswerDrafts(any(), eq(questionSetId))).willReturn(List.of(draft1, draft2));

		// when & then
		mockMvc.perform(
				get("/api/v1/question-sets/{questionSetId}/study-mode/drafts", questionSetId))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data[0].solvingSessionId").value(100),
				jsonPath("$.data[0].questionId").value(11),
				jsonPath("$.data[0].submitted").value(true),
				jsonPath("$.data[1].questionId").value(12),
				jsonPath("$.data[1].submitted").value(false));

		verify(studyModeService).getStudyAnswerDrafts(any(), eq(questionSetId));
	}

	@Test
	@DisplayName("학습모드 답안 초안 업데이트 성공")
	void updateStudyDraft_Success() throws Exception {
		// given
		Long questionSetId = 1L;
		Long questionId = 11L;

		StudyAnswerDraftDto updatedDraft = StudyAnswerDraftDto.builder()
			.solvingSessionId(100L)
			.questionId(questionId)
			.submittedAnswer("{\"submitAnswers\":[\"답안\"],\"type\":\"SHORT\"}")
			.submitted(true)
			.build();

		given(studyModeService.updateStudyAnswerDraft(any(), eq(questionSetId), eq(questionId),
			any(SubmitAnswerDto.class))).willReturn(updatedDraft);

		// when & then
		mockMvc.perform(
				patch("/api/v1/question-sets/{questionSetId}/study-mode/drafts/{questionId}",
					questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"type\":\"SHORT\",\"submitAnswers\":[\"답안\"]}"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.solvingSessionId").value(100),
				jsonPath("$.data.questionId").value(questionId),
				jsonPath("$.data.submitted").value(true));

		verify(studyModeService).updateStudyAnswerDraft(any(), eq(questionSetId), eq(questionId),
			any(SubmitAnswerDto.class));
	}

	@Test
	@DisplayName("학습모드 답안 초안 업데이트 실패 - 존재하지 않는 리소스")
	void updateStudyDraft_Fail_EntityNotFound() throws Exception {
		// given
		Long questionSetId = 1L;
		Long questionId = 11L;

		given(studyModeService.updateStudyAnswerDraft(any(), eq(questionSetId), eq(questionId),
			any(SubmitAnswerDto.class))).willThrow(new EntityNotFoundException("존재하지 않는 문제 셋 입니다."));

		// when & then
		mockMvc.perform(
				patch("/api/v1/question-sets/{questionSetId}/study-mode/drafts/{questionId}",
					questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"type\":\"SHORT\",\"submitAnswers\":[\"답안\"]}"))
			.andExpectAll(
				status().isNotFound(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-002"),
				jsonPath("$.reasons[0]").value("존재하지 않는 문제 셋 입니다."));
	}

	@Test
	@DisplayName("학습모드 답안 초안 업데이트 실패 - 권한 없음")
	void updateStudyDraft_Fail_UserRoleForbidden() throws Exception {
		// given
		Long questionSetId = 1L;
		Long questionId = 11L;

		given(studyModeService.updateStudyAnswerDraft(any(), eq(questionSetId), eq(questionId),
			any(SubmitAnswerDto.class))).willThrow(new UserRoleException("해당 문제를 풀 수 있는 권한이 없습니다."));

		// when & then
		mockMvc.perform(
				patch("/api/v1/question-sets/{questionSetId}/study-mode/drafts/{questionId}",
					questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"type\":\"SHORT\",\"submitAnswers\":[\"답안\"]}"))
			.andExpectAll(
				status().isForbidden(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-008"),
				jsonPath("$.reasons[0]").value("해당 문제를 풀 수 있는 권한이 없습니다."));
	}
}
