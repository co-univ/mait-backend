package com.coniv.mait.web.question.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetOngoingStatus;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.enums.QuestionValidationResult;
import com.coniv.mait.domain.question.service.QuestionSetMaterialService;
import com.coniv.mait.domain.question.service.QuestionSetService;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.domain.question.service.dto.QuestionSetMaterialDto;
import com.coniv.mait.domain.question.service.dto.QuestionValidateDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.question.dto.QuestionSetGroup;
import com.coniv.mait.web.question.dto.QuestionSetList;
import com.coniv.mait.web.question.dto.UpdateQuestionSetApiRequest;
import com.coniv.mait.web.question.dto.UpdateQuestionSetFieldApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = QuestionSetController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionSetControllerTest {

	@MockitoBean
	private QuestionSetService questionSetService;

	@MockitoBean
	private QuestionSetMaterialService questionSetMaterialService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);
	}

	// @Test
	// @DisplayName("문제 셋 생성 테스트")
	// void createQuestionSetTest() throws Exception {
	// 	// given
	// 	final Long questionSetId = 1L;
	// 	String subject = "Sample Subject";
	// 	QuestionSetCreationType creationType = QuestionSetCreationType.MANUAL;
	// 	CreateQuestionSetApiRequest request = new CreateQuestionSetApiRequest(subject,
	// 		creationType);
	//
	// 	QuestionSetDto questionSetDto = QuestionSetDto.builder()
	// 		.id(questionSetId)
	// 		.subject(subject)
	// 		.build();
	//
	// 	when(questionSetService.createQuestionSet(subject, creationType)).thenReturn(questionSetDto);
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/question-sets")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isCreated())
	// 		.andExpect(jsonPath("$.data.questionSetId").value(questionSetId))
	// 		.andExpect(jsonPath("$.data.subject").value(subject));
	//
	// 	// then
	// 	verify(questionSetService).createQuestionSet(request.subject(), request.creationType());
	// }

	// @ParameterizedTest(name = "{index} - {0}")
	// @DisplayName("문제 셋 생성 실패 테스트 - 유효하지 않은 요청")
	// @MethodSource("invalidCreateQuestionSetRequests")
	// void createQuestionSetInvalidRequestTest(String testName, String subject, QuestionSetCreationType creationType,
	// 	String expectedMessage) throws Exception {
	// 	// given
	// 	CreateQuestionSetApiRequest request = new CreateQuestionSetApiRequest(subject, creationType);
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/question-sets")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpectAll(
	// 			status().isBadRequest(),
	// 			jsonPath("$.isSuccess").value(false),
	// 			jsonPath("$.code").value("C-001"),
	// 			jsonPath("$.message").value("사용자 입력 오류입니다."),
	// 			jsonPath("$.reasons").isArray(),
	// 			jsonPath("$.reasons[0]").value(expectedMessage));
	//
	// 	verify(questionSetService, never()).createQuestionSet(anyString(), any());
	// }

	static Stream<Arguments> invalidCreateQuestionSetRequests() {
		return Stream.of(
			Arguments.of("빈 제목", "", QuestionSetCreationType.MANUAL, "교육 주제를 입력해주세요."),
			Arguments.of("null 제목", null, QuestionSetCreationType.MANUAL, "교육 주제를 입력해주세요."),
			Arguments.of("공백만 있는 제목", "   ", QuestionSetCreationType.MANUAL, "교육 주제를 입력해주세요."),
			Arguments.of("빈 생성 타입", "Valid Subject", null, "문제 셋 생성 유형을 선택해주세요."));
	}

	// @Test
	// @DisplayName("문제 셋 생성 실패 테스트 - 여러 필드 동시 유효성 검증 실패")
	// void createQuestionSetMultipleValidationFailuresTest() throws Exception {
	// 	// given
	// 	CreateQuestionSetApiRequest request = new CreateQuestionSetApiRequest(null, null);
	//
	// 	// when & then
	// 	mockMvc.perform(post("/api/v1/question-sets")
	// 			.contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpectAll(
	// 			status().isBadRequest(),
	// 			jsonPath("$.isSuccess").value(false),
	// 			jsonPath("$.code").value("C-001"),
	// 			jsonPath("$.message").value("사용자 입력 오류입니다."),
	// 			jsonPath("$.reasons").isArray(),
	// 			jsonPath("$.reasons.length()").value(2),
	// 			jsonPath("$.reasons[*]").value(org.hamcrest.Matchers.hasItems(
	// 				"교육 주제를 입력해주세요.",
	// 				"문제 셋 생성 유형을 선택해주세요.")));
	//
	// 	verify(questionSetService, never()).createQuestionSet(anyString(), any());
	// }

	@Test
	@DisplayName("문제 셋 목록 조회 테스트 - MAKING 모드는 List 구조 반환")
	void getQuestionSets_MakingMode_ReturnsList() throws Exception {
		// given
		Long teamId = 1L;
		final DeliveryMode mode = DeliveryMode.MAKING;
		QuestionSetDto questionSet1 = QuestionSetDto.builder()
			.id(1L)
			.subject("Subject 1")
			.build();
		QuestionSetDto questionSet2 = QuestionSetDto.builder()
			.id(2L)
			.subject("Subject 2")
			.build();

		QuestionSetList questionSetList = QuestionSetList.of(List.of(questionSet1, questionSet2));
		when(questionSetService.getQuestionSets(teamId, mode)).thenReturn(questionSetList);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets")
				.param("teamId", String.valueOf(teamId))
				.param("mode", mode.name()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.mode").value("MAKING"))
			.andExpect(jsonPath("$.data.content.questionSets.length()").value(2))
			.andExpect(jsonPath("$.data.content.questionSets[0].id").value(1L))
			.andExpect(jsonPath("$.data.content.questionSets[0].subject").value("Subject 1"))
			.andExpect(jsonPath("$.data.content.questionSets[1].id").value(2L))
			.andExpect(jsonPath("$.data.content.questionSets[1].subject").value("Subject 2"));

		verify(questionSetService).getQuestionSets(teamId, mode);
	}

	@Test
	@DisplayName("문제 셋 목록 조회 테스트 - LIVE_TIME 모드는 Map 구조 반환")
	void getQuestionSets_LiveTimeMode_ReturnsGroupedMap() throws Exception {
		// given
		Long teamId = 1L;
		final DeliveryMode mode = DeliveryMode.LIVE_TIME;
		QuestionSetDto beforeSet = QuestionSetDto.builder()
			.id(1L)
			.subject("Subject 1")
			.ongoingStatus(QuestionSetOngoingStatus.BEFORE)
			.build();
		QuestionSetDto ongoingSet = QuestionSetDto.builder()
			.id(2L)
			.subject("Subject 2")
			.ongoingStatus(QuestionSetOngoingStatus.ONGOING)
			.build();

		QuestionSetGroup questionSetGroup = QuestionSetGroup.of(List.of(beforeSet, ongoingSet));
		when(questionSetService.getQuestionSets(teamId, mode)).thenReturn(questionSetGroup);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets")
				.param("teamId", String.valueOf(teamId))
				.param("mode", mode.name()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.mode").value("LIVE_TIME"))
			.andExpect(jsonPath("$.data.content.questionSets.BEFORE").isArray())
			.andExpect(jsonPath("$.data.content.questionSets.BEFORE.length()").value(1))
			.andExpect(jsonPath("$.data.content.questionSets.BEFORE[0].id").value(1L))
			.andExpect(jsonPath("$.data.content.questionSets.BEFORE[0].subject").value("Subject 1"))
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING").isArray())
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING.length()").value(1))
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING[0].id").value(2L))
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING[0].subject").value("Subject 2"));

		verify(questionSetService).getQuestionSets(teamId, mode);
	}

	@Test
	@DisplayName("문제 셋 단건 조회 테스트")
	void getQuestionSetTest() throws Exception {
		// given
		final Long questionSetId = 1L;
		final String subject = "Test Subject";
		QuestionSetDto questionSetDto = QuestionSetDto.builder()
			.id(questionSetId)
			.subject(subject)
			.build();

		when(questionSetService.getQuestionSet(questionSetId)).thenReturn(questionSetDto);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}", questionSetId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(questionSetId))
			.andExpect(jsonPath("$.data.subject").value(subject));

		verify(questionSetService).getQuestionSet(questionSetId);
	}

	@Test
	@DisplayName("문제 셋 저장 테스트")
	void updateQuestionSets() throws Exception {
		// given
		final Long questionSetId = 1L;
		final String subject = "Updated Subject";
		final String title = "Updated Title";
		final DeliveryMode mode = DeliveryMode.REVIEW;
		final String levelDescription = "Intermediate";
		final QuestionSetVisibility visibility = QuestionSetVisibility.PRIVATE;

		var request = new UpdateQuestionSetApiRequest(title, subject, mode, levelDescription, visibility);

		QuestionSetDto questionSetDto = QuestionSetDto.builder()
			.id(questionSetId)
			.subject(subject)
			.title(title)
			.deliveryMode(mode)
			.difficulty(levelDescription)
			.visibility(visibility)
			.build();

		when(questionSetService.completeQuestionSet(questionSetId, title, subject, mode, levelDescription, visibility))
			.thenReturn(questionSetDto);

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(questionSetId))
			.andExpect(jsonPath("$.data.subject").value(subject))
			.andExpect(jsonPath("$.data.title").value(title))
			.andExpect(jsonPath("$.data.deliveryMode").value(mode.name()))
			.andExpect(jsonPath("$.data.levelDescription").value(levelDescription))
			.andExpect(jsonPath("$.data.visibility").value(visibility.name()));

		verify(questionSetService).completeQuestionSet(questionSetId, title, subject, mode, levelDescription,
			visibility);
	}

	@ParameterizedTest(name = "{index} - {0}")
	@DisplayName("문제 셋 최종 저장 테스트 - 유효하지 않은 요청")
	@MethodSource("invalidUpdateQuestionSetRequests")
	void updateQuestionSets_InvalidRequest(String testName, UpdateQuestionSetApiRequest request,
		List<String> expectedErrorMessages) throws Exception {
		// given
		final Long questionSetId = 1L;

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons.length()").value(expectedErrorMessages.size()),
				jsonPath("$.reasons[*]").value(org.hamcrest.Matchers.hasItems(
					expectedErrorMessages.toArray(new String[0]))));

		verify(questionSetService, never()).completeQuestionSet(anyLong(), anyString(), anyString(), any(), anyString(),
			any());
	}

	static Stream<Arguments> invalidUpdateQuestionSetRequests() {
		return Stream.of(
			Arguments.of(
				"제목과 주제가 빈 문자열",
				new UpdateQuestionSetApiRequest("", "", DeliveryMode.LIVE_TIME, "설명",
					QuestionSetVisibility.GROUP),
				List.of("제목을 입력해주세요", "주제를 입력해주세요")),
			Arguments.of(
				"제목만 빈 문자열",
				new UpdateQuestionSetApiRequest("", "유효한 주제", DeliveryMode.REVIEW, "설명",
					QuestionSetVisibility.PRIVATE),
				List.of("제목을 입력해주세요")),
			Arguments.of(
				"주제만 빈 문자열",
				new UpdateQuestionSetApiRequest("유효한 제목", "", DeliveryMode.LIVE_TIME, "설명",
					QuestionSetVisibility.GROUP),
				List.of("주제를 입력해주세요")),
			Arguments.of(
				"제목과 주제가 null",
				new UpdateQuestionSetApiRequest(null, null, DeliveryMode.REVIEW, "설명",
					QuestionSetVisibility.GROUP),
				List.of("제목을 입력해주세요", "주제를 입력해주세요")),
			Arguments.of(
				"제목이 공백만 포함",
				new UpdateQuestionSetApiRequest("   ", "유효한 주제", DeliveryMode.LIVE_TIME, "설명",
					QuestionSetVisibility.PRIVATE),
				List.of("제목을 입력해주세요")),
			Arguments.of(
				"주제가 공백만 포함",
				new UpdateQuestionSetApiRequest("유효한 제목", "   ", DeliveryMode.REVIEW, "설명",
					QuestionSetVisibility.GROUP),
				List.of("주제를 입력해주세요")));
	}

	@Test
	@DisplayName("문제 셋 수정 성공 테스트")
	void updateQuestionSetTitle_isSuccess() throws Exception {
		// given
		final Long questionSetId = 1L;
		final String newTitle = "New Title";
		UpdateQuestionSetFieldApiRequest request = new UpdateQuestionSetFieldApiRequest(newTitle);

		// when & then
		mockMvc.perform(patch("/api/v1/question-sets/{questionSetId}", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		// then
		verify(questionSetService).updateQuestionSetField(questionSetId, newTitle);
	}

	@Test
	@DisplayName("문제 셋 수정 실패 테스트 - 유효하지 않은 dto")
	void updateQuestionSetTitle_fail() throws Exception {
		// given
		final Long questionSetId = 1L;
		final String newTitle = "";
		UpdateQuestionSetFieldApiRequest request = new UpdateQuestionSetFieldApiRequest(newTitle);

		// when & then
		mockMvc.perform(patch("/api/v1/question-sets/{questionSetId}", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(status().is4xxClientError(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.reasons[0]").value("문제 셋 제목은 비어있을 수 없습니다."));
	}

	@Test
	@DisplayName("문제 셋 검증 API 테스트 - 모든 문제가 유효한 경우")
	void validateQuestionSet_AllValid() throws Exception {
		// given
		final Long questionSetId = 1L;

		when(questionSetService.validateQuestionSet(questionSetId)).thenReturn(List.of());

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/validate")
				.param("questionSetId", String.valueOf(questionSetId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(0));

		verify(questionSetService).validateQuestionSet(questionSetId);
	}

	@Test
	@DisplayName("문제 셋 검증 API 테스트 - 일부 문제가 유효하지 않은 경우")
	void validateQuestionSet_SomeInvalid() throws Exception {
		// given
		final Long questionSetId = 1L;

		QuestionValidateDto invalidDto1 = QuestionValidateDto.builder()
			.questionId(2L)
			.number(2L)
			.valid(false)
			.reason(QuestionValidationResult.EMPTY_CONTENT)
			.build();

		QuestionValidateDto invalidDto2 = QuestionValidateDto.builder()
			.questionId(3L)
			.number(3L)
			.valid(false)
			.reason(QuestionValidationResult.INVALID_CHOICE_COUNT)
			.build();

		when(questionSetService.validateQuestionSet(questionSetId))
			.thenReturn(List.of(invalidDto1, invalidDto2));

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/validate")
				.param("questionSetId", String.valueOf(questionSetId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].questionId").value(2L))
			.andExpect(jsonPath("$.data[0].isValid").value(false))
			.andExpect(jsonPath("$.data[0].number").value(2L))
			.andExpect(jsonPath("$.data[1].questionId").value(3L))
			.andExpect(jsonPath("$.data[1].isValid").value(false))
			.andExpect(jsonPath("$.data[1].number").value(3L));

		verify(questionSetService).validateQuestionSet(questionSetId);
	}

	@Test
	@DisplayName("문제 셋 자료 파일 업로드 API 테스트 - 성공")
	void uploadQuestionSetMaterial_Success() throws Exception {
		// given
		final Long questionSetId = 1L;
		final Long materialId = 100L;
		final String fileName = "test-material.pdf";
		final String fileUrl = "https://s3.amazonaws.com/bucket/test-material.pdf";
		final String fileKey = "question-set-materials/test-material.pdf";

		MockMultipartFile mockFile = new MockMultipartFile(
			"material",
			fileName,
			MediaType.APPLICATION_PDF_VALUE,
			"test file content".getBytes());

		QuestionSetMaterialDto mockResponse = QuestionSetMaterialDto.builder()
			.id(materialId)
			.materialUrl(fileUrl)
			.materialKey(fileKey)
			.build();

		when(questionSetMaterialService.uploadQuestionSetMaterial(any()))
			.thenReturn(mockResponse);

		// when & then
		mockMvc.perform(multipart("/api/v1/question-sets/materials", questionSetId)
				.file(mockFile))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.id").value(materialId))
			.andExpect(jsonPath("$.data.materialUrl").value(fileUrl));

		verify(questionSetMaterialService).uploadQuestionSetMaterial(any());
	}
}
