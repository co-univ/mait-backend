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
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.web.question.dto.CreateMultipleQuestionApiRequest;
import com.coniv.mait.web.question.dto.CreateOrderingQuestionApiRequest;
import com.coniv.mait.web.question.dto.CreateShortQuestionApiRequest;
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

	@Test
	@DisplayName("문제 셋에 주관식 문제 저장 API 테스트")
	void createShortQuestionTest() throws Exception {
		// given
		Long questionSetId = 1L;
		String type = "SHORT";
		var shortAnswers = List.of(
			ShortAnswerDto.builder()
				.answer("정답1")
				.isMain(true)
				.number(1L)
				.build(),
			ShortAnswerDto.builder()
				.answer("정답2")
				.isMain(false)
				.number(2L)
				.build()
		);
		var request = new CreateShortQuestionApiRequest();
		request.setContent("주관식 문제 내용");
		request.setExplanation("문제 해설");
		request.setNumber(1L);
		request.setShortAnswers(shortAnswers);
		// type 필드 추가
		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"SHORT\",");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type={type}", questionSetId, type)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		verify(questionService).createQuestion(eq(questionSetId),
			eq(com.coniv.mait.domain.question.enums.QuestionType.SHORT), any());
	}

	@ParameterizedTest(name = "{index} - {0}")
	@DisplayName("주관식 문제 생성 실패 테스트 - 유효하지 않은 요청")
	@MethodSource("invalidCreateShortQuestionRequests")
	void createShortQuestionInvalidRequestTest(
		String testName,
		CreateShortQuestionApiRequest request,
		String expectedMessage
	) throws Exception {
		// given
		Long questionSetId = 1L;
		String type = "SHORT";
		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"SHORT\",");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type={type}", questionSetId, type)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons[0]").value(expectedMessage)
			);

		verify(questionService, never()).createQuestion(anyLong(), any(), any());
	}

	static Stream<Arguments> invalidCreateShortQuestionRequests() {
		return Stream.of(
			Arguments.of("문제 번호 누락", new CreateShortQuestionApiRequest() {
				{
					setContent("주관식 문제 내용");
					setExplanation("문제 해설");
					setShortAnswers(List.of(
						ShortAnswerDto.builder().answer("정답1").isMain(true).number(1L).build()
					));
					setNumber(null);
				}
			}, "문제 번호는 필수입니다."),
			Arguments.of("정답 리스트 빈 배열", new CreateShortQuestionApiRequest() {
				{
					setContent("주관식 문제 내용");
					setExplanation("문제 해설");
					setNumber(1L);
					setShortAnswers(List.of());
				}
			}, "정답은 최소 1개 이상이어야 합니다.")
		);
	}

	@Test
	@DisplayName("문제 셋에 순서배열 문제 저장 API 테스트 - 성공")
	void createOrderingQuestionTest() throws Exception {
		// given
		Long questionSetId = 1L;
		String type = "ORDERING";
		var options = List.of(
			OrderingQuestionOptionDto.builder()
				.content("첫 번째 옵션")
				.originOrder(1)
				.answerOrder(2)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("두 번째 옵션")
				.originOrder(2)
				.answerOrder(1)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("세 번째 옵션")
				.originOrder(3)
				.answerOrder(3)
				.build()
		);
		var request = new CreateOrderingQuestionApiRequest();
		request.setContent("순서배열 문제 내용");
		request.setExplanation("문제 해설");
		request.setNumber(1L);
		request.setOptions(options);

		// type 필드 추가
		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"ORDERING\",");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type={type}", questionSetId, type)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		verify(questionService).createQuestion(eq(questionSetId),
			eq(com.coniv.mait.domain.question.enums.QuestionType.ORDERING), any());
	}

	@Test
	@DisplayName("순서배열 문제 생성 실패 테스트 - 선택지가 null인 경우")
	void createOrderingQuestionNullOptionsTest() throws Exception {
		// given
		Long questionSetId = 1L;
		String type = "ORDERING";
		var request = new CreateOrderingQuestionApiRequest();
		request.setContent("순서배열 문제 내용");
		request.setExplanation("문제 해설");
		request.setNumber(1L);
		request.setOptions(null); // null로 설정

		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"ORDERING\",");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type={type}", questionSetId, type)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons[0]").value("선택지는 필수입니다.")
			);

		verify(questionService, never()).createQuestion(anyLong(), any(), any());
	}

	@Test
	@DisplayName("순서배열 문제 생성 실패 테스트 - 문제 번호가 null인 경우")
	void createOrderingQuestionNullNumberTest() throws Exception {
		// given
		Long questionSetId = 1L;
		String type = "ORDERING";
		var options = List.of(
			OrderingQuestionOptionDto.builder()
				.content("첫 번째 옵션")
				.originOrder(1)
				.answerOrder(2)
				.build()
		);
		var request = new CreateOrderingQuestionApiRequest();
		request.setContent("순서배열 문제 내용");
		request.setExplanation("문제 해설");
		request.setNumber(null); // null로 설정
		request.setOptions(options);

		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"ORDERING\",");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type={type}", questionSetId, type)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons[0]").value("문제 번호는 필수입니다.")
			);

		verify(questionService, never()).createQuestion(anyLong(), any(), any());
	}
}
