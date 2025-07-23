package com.coniv.mait.web.question.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.web.question.dto.CreateMultipleQuestionApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = QuestionController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionControllerTest {

	@MockitoBean
	private QuestionService questionService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	@DisplayName("문제 셋에 객관식 문제 저장 API 테스트")
	void createMultipleQuestionTest() throws Exception {
		// given
		Long questionSetId = 1L;
		List<MultipleChoiceDto> choices = List.of(
			MultipleChoiceDto.builder()
				.number(1)
				.content("선택지 1")
				.isCorrect(true)
				.build(),
			MultipleChoiceDto.builder()
				.number(2)
				.content("선택지 2")
				.isCorrect(false)
				.build()
		);

		CreateMultipleQuestionApiRequest request = new CreateMultipleQuestionApiRequest(
			"Sample Question", "Sample Explanation", 1L, choices);
		String json = objectMapper.writeValueAsString(request);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type=multiple", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		verify(questionService).createMultipleQuestion(questionSetId, request.multipleQuestionDto());
	}

	@ParameterizedTest(name = "{index} - {0}")
	@DisplayName("객관식 문제 생성 실패 테스트 - 유효하지 않은 요청")
	@MethodSource("invalidCreateMultipleQuestionRequests")
	void createMultipleQuestionInvalidRequestTest(String testName, List<MultipleChoiceDto> choices,
		String expectedMessage) throws Exception {
		// given
		Long questionSetId = 1L;
		CreateMultipleQuestionApiRequest request = new CreateMultipleQuestionApiRequest(
			"Sample Question", "Sample Explanation", 1L, choices);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type=multiple", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons[0]").value(expectedMessage)
			);

		verify(questionService, never()).createMultipleQuestion(anyLong(), any());
	}

	static Stream<Arguments> invalidCreateMultipleQuestionRequests() {
		return Stream.of(
			Arguments.of("null 선택지", null, "객관식 문제에는 반드시 선지가 있어야 합니다."),
			Arguments.of("빈 선택지 리스트", List.of(), "객관식 문제는 최소 2개, 최대 8개의 선택지를 가져야 합니다."),
			Arguments.of("선택지 1개", List.of(
				MultipleChoiceDto.builder()
					.number(1)
					.content("선택지 1")
					.isCorrect(true)
					.build()
			), "객관식 문제는 최소 2개, 최대 8개의 선택지를 가져야 합니다."),
			Arguments.of("선택지 9개", List.of(
				MultipleChoiceDto.builder().number(1).content("선택지 1").isCorrect(true).build(),
				MultipleChoiceDto.builder().number(2).content("선택지 2").isCorrect(false).build(),
				MultipleChoiceDto.builder().number(3).content("선택지 3").isCorrect(false).build(),
				MultipleChoiceDto.builder().number(4).content("선택지 4").isCorrect(false).build(),
				MultipleChoiceDto.builder().number(5).content("선택지 5").isCorrect(false).build(),
				MultipleChoiceDto.builder().number(6).content("선택지 6").isCorrect(false).build(),
				MultipleChoiceDto.builder().number(7).content("선택지 7").isCorrect(false).build(),
				MultipleChoiceDto.builder().number(8).content("선택지 8").isCorrect(false).build(),
				MultipleChoiceDto.builder().number(9).content("선택지 9").isCorrect(false).build()
			), "객관식 문제는 최소 2개, 최대 8개의 선택지를 가져야 합니다.")
		);
	}

	@Test
	@DisplayName("객관식 문제 생성 실패 테스트 - 선택지 내용이 유효하지 않은 경우")
	void createMultipleQuestionInvalidChoiceContentTest() throws Exception {
		// given
		Long questionSetId = 1L;
		List<MultipleChoiceDto> choices = List.of(
			MultipleChoiceDto.builder()
				.number(1)
				.content("") // 빈 내용
				.isCorrect(true)
				.build(),
			MultipleChoiceDto.builder()
				.number(2)
				.content("선택지 2")
				.isCorrect(false)
				.build()
		);

		CreateMultipleQuestionApiRequest request = new CreateMultipleQuestionApiRequest(
			"Sample Question", "Sample Explanation", 1L, choices);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type=multiple", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons[0]").value("객관식 선지의 내용은 필수입니다.")
			);

		verify(questionService, never()).createMultipleQuestion(anyLong(), any());
	}
}
