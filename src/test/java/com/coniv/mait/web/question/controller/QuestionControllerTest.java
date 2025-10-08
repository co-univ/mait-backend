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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.question.service.QuestionImageService;
import com.coniv.mait.domain.question.service.QuestionService;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.MultipleQuestionDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.domain.question.service.dto.ShortQuestionDto;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.question.dto.CreateDefaultQuestionApiRequest;
import com.coniv.mait.web.question.dto.CreateFillBlankQuestionApiRequest;
import com.coniv.mait.web.question.dto.CreateMultipleQuestionApiRequest;
import com.coniv.mait.web.question.dto.CreateOrderingQuestionApiRequest;
import com.coniv.mait.web.question.dto.CreateShortQuestionApiRequest;
import com.coniv.mait.web.question.dto.UpdateFillBlankQuestionApiRequest;
import com.coniv.mait.web.question.dto.UpdateMultipleQuestionApiRequest;
import com.coniv.mait.web.question.dto.UpdateOrderingQuestionApiRequest;
import com.coniv.mait.web.question.dto.UpdateQuestionOrderApiRequest;
import com.coniv.mait.web.question.dto.UpdateShortQuestionApiRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@WebMvcTest(controllers = QuestionController.class)
@AutoConfigureMockMvc(addFilters = false)
class QuestionControllerTest {

	@MockitoBean
	private QuestionService questionService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@MockitoBean
	private QuestionImageService questionImageService;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);
	}

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

		CreateMultipleQuestionApiRequest request = new CreateMultipleQuestionApiRequest();
		request.setContent("Sample Question");
		request.setExplanation("Sample Explanation");
		request.setNumber(1L);
		request.setChoices(choices);

		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"MULTIPLE\",");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type=MULTIPLE", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		verify(questionService).createQuestion(eq(questionSetId),
			eq(com.coniv.mait.domain.question.enums.QuestionType.MULTIPLE), any());
	}

	@ParameterizedTest(name = "{index} - {0}")
	@DisplayName("객관식 문제 생성 실패 테스트 - 유효하지 않은 요청")
	@MethodSource("invalidCreateMultipleQuestionRequests")
	void createMultipleQuestionInvalidRequestTest(String testName, List<MultipleChoiceDto> choices,
		String expectedMessage) throws Exception {
		// given
		Long questionSetId = 1L;
		CreateMultipleQuestionApiRequest request = new CreateMultipleQuestionApiRequest();
		request.setContent("Sample Question");
		request.setExplanation("Sample Explanation");
		request.setNumber(1L);
		request.setChoices(choices);

		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"MULTIPLE\",");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type=MULTIPLE", questionSetId)
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

		CreateMultipleQuestionApiRequest request = new CreateMultipleQuestionApiRequest();
		request.setContent("Sample Question");
		request.setExplanation("Sample Explanation");
		request.setNumber(1L);
		request.setChoices(choices);

		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"MULTIPLE\",");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type=MULTIPLE", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-001"),
				jsonPath("$.message").value("사용자 입력 오류입니다."),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons[0]").value("객관식 선지의 내용은 필수입니다.")
			);

		verify(questionService, never()).createQuestion(anyLong(), any(), any());
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

	@Test
	@DisplayName("문제 셋에 빈칸 문제 저장 API 테스트 - 성공")
	void createFillBlankQuestionTest() throws Exception {
		// given
		Long questionSetId = 1L;
		String type = "FILL_BLANK";
		var fillBlankAnswers = List.of(
			FillBlankAnswerDto.builder()
				.answer("정답1")
				.isMain(true)
				.number(1L)
				.build(),
			FillBlankAnswerDto.builder()
				.answer("정답2")
				.isMain(false)
				.number(2L)
				.build()
		);
		var request = new CreateFillBlankQuestionApiRequest();
		request.setContent("빈칸에 들어갈 적절한 단어는 ___입니다.");
		request.setExplanation("문제 해설");
		request.setNumber(1L);
		request.setFillBlankAnswers(fillBlankAnswers);

		// type 필드 추가
		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"FILL_BLANK\",");

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type={type}", questionSetId, type)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		verify(questionService).createQuestion(eq(questionSetId),
			eq(com.coniv.mait.domain.question.enums.QuestionType.FILL_BLANK), any());
	}

	@Test
	@DisplayName("빈칸 문제 생성 실패 테스트 - 정답이 null인 경우")
	void createFillBlankQuestionNullAnswersTest() throws Exception {
		// given
		Long questionSetId = 1L;
		String type = "FILL_BLANK";
		var request = new CreateFillBlankQuestionApiRequest();
		request.setContent("빈칸에 들어갈 적절한 단어는 ___입니다.");
		request.setExplanation("문제 해설");
		request.setNumber(1L);
		request.setFillBlankAnswers(null); // null로 설정

		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"FILL_BLANK\",");

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
				jsonPath("$.reasons[0]").value("빈칸 문제 정답은 필수입니다.")
			);

		verify(questionService, never()).createQuestion(anyLong(), any(), any());
	}

	@Test
	@DisplayName("빈칸 문제 생성 실패 테스트 - 문제 번호가 null인 경우")
	void createFillBlankQuestionNullNumberTest() throws Exception {
		// given
		Long questionSetId = 1L;
		String type = "FILL_BLANK";
		var fillBlankAnswers = List.of(
			FillBlankAnswerDto.builder()
				.answer("정답1")
				.isMain(true)
				.number(1L)
				.build()
		);
		var request = new CreateFillBlankQuestionApiRequest();
		request.setContent("빈칸에 들어갈 적절한 단어는 ___입니다.");
		request.setExplanation("문제 해설");
		request.setNumber(null); // null로 설정
		request.setFillBlankAnswers(fillBlankAnswers);

		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"FILL_BLANK\",");

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

	@Test
	@DisplayName("문제 조회 API 테스트 - 성공")
	void getQuestionTest() throws Exception {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		MultipleQuestionDto mockQuestionDto = MultipleQuestionDto.builder()
			.id(questionId)
			.content("객관식 문제 내용")
			.explanation("문제 해설")
			.number(1L)
			.choices(List.of(
				MultipleChoiceDto.builder()
					.number(1)
					.content("선택지 1")
					.isCorrect(true)
					.build()
			))
			.build();

		when(questionService.getQuestion(questionSetId, questionId, null)).thenReturn(mockQuestionDto);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}", questionSetId, questionId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").exists(),
				jsonPath("$.data.content").value("객관식 문제 내용")
			);

		verify(questionService).getQuestion(questionSetId, questionId, null);
	}

	@Test
	@DisplayName("문제 조회 API 테스트 - 존재하지 않는 문제")
	void getQuestionNotFoundTest() throws Exception {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 999L;

		when(questionService.getQuestion(questionSetId, questionId, null))
			.thenThrow(new EntityNotFoundException("Question not found with id: " + questionId));

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}", questionSetId, questionId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isNotFound(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-002"),
				jsonPath("$.message").value("특정 엔티티를 조회할 수 없습니다."),
				jsonPath("$.reasons").value("Question not found with id: " + questionId)
			);

		verify(questionService).getQuestion(questionSetId, questionId, null);
	}

	@Test
	@DisplayName("문제 조회 API 테스트 - 해당 문제셋에 속하지 않는 문제")
	void getQuestionNotBelongToQuestionSetTest() throws Exception {
		// given
		final Long questionSetId = 1L;
		final Long questionId = 1L;

		when(questionService.getQuestion(questionSetId, questionId, null))
			.thenThrow(new ResourceNotBelongException("해당 문제 셋에 속한 문제가 아닙니다."));

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}", questionSetId, questionId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-004"),
				jsonPath("$.message").value("리소스가 소속되지 않았습니다."),
				jsonPath("$.reasons").value("해당 문제 셋에 속한 문제가 아닙니다.")
			);

		verify(questionService).getQuestion(questionSetId, questionId, null);
	}

	@Test
	@DisplayName("문제 셋의 모든 문제 조회 API 테스트 - 성공")
	void getQuestionsTest() throws Exception {
		// given
		final Long questionSetId = 1L;

		// 다양한 타입의 문제 DTO들을 생성
		MultipleQuestionDto multipleQuestionDto = MultipleQuestionDto.builder()
			.id(1L)
			.content("객관식 문제 1")
			.explanation("객관식 문제 해설")
			.number(1L)
			.choices(List.of(
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
			))
			.build();

		ShortAnswerDto shortAnswerDto = ShortAnswerDto.builder()
			.answer("주관식 정답")
			.isMain(true)
			.number(1L)
			.build();

		com.coniv.mait.domain.question.service.dto.ShortQuestionDto shortQuestionDto =
			com.coniv.mait.domain.question.service.dto.ShortQuestionDto.builder()
				.id(2L)
				.content("주관식 문제 1")
				.explanation("주관식 문제 해설")
				.number(2L)
				.shortAnswers(List.of(shortAnswerDto))
				.build();

		List<com.coniv.mait.domain.question.service.dto.QuestionDto> mockQuestions =
			List.of(multipleQuestionDto, shortQuestionDto);

		when(questionService.getQuestions(questionSetId)).thenReturn(mockQuestions);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions", questionSetId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").isArray(),
				jsonPath("$.data.length()").value(2),
				jsonPath("$.data[0].content").value("객관식 문제 1"),
				jsonPath("$.data[0].number").value(1),
				jsonPath("$.data[1].content").value("주관식 문제 1"),
				jsonPath("$.data[1].number").value(2)
			);

		verify(questionService).getQuestions(questionSetId);
	}

	@Test
	@DisplayName("문제 셋의 모든 문제 조회 API 테스트 - 빈 목록")
	void getQuestionsEmptyListTest() throws Exception {
		// given
		final Long questionSetId = 1L;

		when(questionService.getQuestions(questionSetId)).thenReturn(List.of());

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions", questionSetId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").isArray(),
				jsonPath("$.data.length()").value(0)
			);

		verify(questionService).getQuestions(questionSetId);
	}

	@Test
	@DisplayName("단답형 문제 수정 API 테스트 - 성공")
	void updateShortQuestionTest() throws Exception {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		var shortAnswers = List.of(
			ShortAnswerDto.builder()
				.answer("수정된 정답1")
				.isMain(true)
				.number(1L)
				.build(),
			ShortAnswerDto.builder()
				.answer("수정된 정답2")
				.isMain(false)
				.number(2L)
				.build()
		);

		UpdateShortQuestionApiRequest request = new UpdateShortQuestionApiRequest();
		request.setId(questionId);
		request.setContent("수정된 주관식 문제 내용");
		request.setExplanation("수정된 문제 해설");
		request.setNumber(1L);
		request.setShortAnswers(shortAnswers);

		String json = objectMapper.writeValueAsString(request);
		// Mock 응답 설정
		ShortQuestionDto mockResponse =
			com.coniv.mait.domain.question.service.dto.ShortQuestionDto.builder()
				.id(questionId)
				.content(request.getContent())
				.explanation(request.getExplanation())
				.number(request.getNumber())
				.shortAnswers(request.getShortAnswers())
				.build();

		when(questionService.updateQuestion(eq(questionSetId), eq(questionId), any())).thenReturn(mockResponse);

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/{questionId}", questionSetId, questionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		verify(questionService).updateQuestion(eq(questionSetId), eq(questionId), any());
	}

	@Test
	@DisplayName("객관식 문제 수정 API 테스트 - 성공")
	void updateMultipleQuestionTest() throws Exception {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		List<MultipleChoiceDto> choices = List.of(
			MultipleChoiceDto.builder()
				.number(1)
				.content("수정된 선택지 1")
				.isCorrect(true)
				.build(),
			MultipleChoiceDto.builder()
				.number(2)
				.content("수정된 선택지 2")
				.isCorrect(false)
				.build(),
			MultipleChoiceDto.builder()
				.number(3)
				.content("수정된 선택지 3")
				.isCorrect(false)
				.build()
		);

		UpdateMultipleQuestionApiRequest request = new UpdateMultipleQuestionApiRequest();
		request.setId(questionId);
		request.setContent("수정된 객관식 문제 내용");
		request.setExplanation("수정된 문제 해설");
		request.setNumber(1L);
		request.setChoices(choices);

		String json = objectMapper.writeValueAsString(request);

		// Mock 응답 설정
		MultipleQuestionDto mockResponse = MultipleQuestionDto.builder()
			.id(questionId)
			.content(request.getContent())
			.explanation(request.getExplanation())
			.number(request.getNumber())
			.choices(request.getChoices())
			.build();

		when(questionService.updateQuestion(eq(questionSetId), eq(questionId), any())).thenReturn(mockResponse);

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/{questionId}", questionSetId, questionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		verify(questionService).updateQuestion(eq(questionSetId), eq(questionId), any());
	}

	@Test
	@DisplayName("순서맞추기 문제 수정 API 테스트 - 성공")
	void updateOrderingQuestionTest() throws Exception {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		var options = List.of(
			OrderingQuestionOptionDto.builder()
				.content("수정된 첫 번째 옵션")
				.originOrder(1)
				.answerOrder(3)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("수정된 두 번째 옵션")
				.originOrder(2)
				.answerOrder(1)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("수정된 세 번째 옵션")
				.originOrder(3)
				.answerOrder(2)
				.build()
		);

		UpdateOrderingQuestionApiRequest request = new UpdateOrderingQuestionApiRequest();
		request.setId(questionId);
		request.setContent("수정된 순서맞추기 문제 내용");
		request.setExplanation("수정된 문제 해설");
		request.setNumber(1L);
		request.setOptions(options);

		String json = objectMapper.writeValueAsString(request);

		// Mock 응답 설정
		com.coniv.mait.domain.question.service.dto.OrderingQuestionDto mockResponse =
			com.coniv.mait.domain.question.service.dto.OrderingQuestionDto.builder()
				.id(questionId)
				.content(request.getContent())
				.explanation(request.getExplanation())
				.number(request.getNumber())
				.options(request.getOptions())
				.build();

		when(questionService.updateQuestion(eq(questionSetId), eq(questionId), any())).thenReturn(mockResponse);

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/{questionId}", questionSetId, questionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		verify(questionService).updateQuestion(eq(questionSetId), eq(questionId), any());
	}

	@Test
	@DisplayName("빈칸채우기 문제 수정 API 테스트 - 성공")
	void updateFillBlankQuestionTest() throws Exception {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		var fillBlankAnswers = List.of(
			FillBlankAnswerDto.builder()
				.answer("수정된 정답1")
				.isMain(true)
				.number(1L)
				.build(),
			FillBlankAnswerDto.builder()
				.answer("수정된 정답2")
				.isMain(false)
				.number(2L)
				.build()
		);

		UpdateFillBlankQuestionApiRequest request = new UpdateFillBlankQuestionApiRequest();
		request.setId(questionId);
		request.setContent("수정된 빈칸에 들어갈 적절한 단어는 ___입니다.");
		request.setExplanation("수정된 문제 해설");
		request.setNumber(1L);
		request.setFillBlankAnswers(fillBlankAnswers);

		String json = objectMapper.writeValueAsString(request);

		// Mock 응답 설정
		com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto mockResponse =
			com.coniv.mait.domain.question.service.dto.FillBlankQuestionDto.builder()
				.id(questionId)
				.content(request.getContent())
				.explanation(request.getExplanation())
				.number(request.getNumber())
				.fillBlankAnswers(request.getFillBlankAnswers())
				.build();

		when(questionService.updateQuestion(eq(questionSetId), eq(questionId), any())).thenReturn(mockResponse);

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/{questionId}", questionSetId, questionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk());

		verify(questionService).updateQuestion(eq(questionSetId), eq(questionId), any());
	}

	@Test
	@DisplayName("문제 수정 API 테스트 - 같은 유형으로 수정 성공 (객관식 -> 객관식)")
	void updateQuestionSameTypeTest() throws Exception {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		List<MultipleChoiceDto> choices = List.of(
			MultipleChoiceDto.builder()
				.number(1)
				.content("수정된 선택지 1")
				.isCorrect(true)
				.build(),
			MultipleChoiceDto.builder()
				.number(2)
				.content("수정된 선택지 2")
				.isCorrect(false)
				.build()
		);

		UpdateMultipleQuestionApiRequest request = new UpdateMultipleQuestionApiRequest();
		request.setId(questionId);
		request.setContent("수정된 객관식 문제 내용");
		request.setExplanation("수정된 문제 해설");
		request.setNumber(1L);
		request.setChoices(choices);

		String json = objectMapper.writeValueAsString(request);

		// Mock 반환값 설정
		MultipleQuestionDto mockResponse = MultipleQuestionDto.builder()
			.id(questionId)
			.content("수정된 객관식 문제 내용")
			.explanation("수정된 문제 해설")
			.number(1L)
			.choices(choices)
			.build();

		when(questionService.updateQuestion(eq(questionSetId), eq(questionId), any())).thenReturn(mockResponse);

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/{questionId}", questionSetId, questionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.content").value("수정된 객관식 문제 내용"),
				jsonPath("$.data.explanation").value("수정된 문제 해설")
			);

		verify(questionService).updateQuestion(eq(questionSetId), eq(questionId), any());
	}

	@Test
	@DisplayName("문제 수정 API 테스트 - 다른 유형으로 변경 성공 (객관식 -> 주관식)")
	void updateQuestionDifferentTypeTest() throws Exception {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		var shortAnswers = List.of(
			ShortAnswerDto.builder()
				.answer("변경된 정답1")
				.isMain(true)
				.number(1L)
				.build(),
			ShortAnswerDto.builder()
				.answer("변경된 정답2")
				.isMain(false)
				.number(2L)
				.build()
		);

		UpdateShortQuestionApiRequest request = new UpdateShortQuestionApiRequest();
		request.setId(questionId);
		request.setContent("객관식에서 주관식으로 변경된 문제");
		request.setExplanation("변경된 해설");
		request.setNumber(1L);
		request.setShortAnswers(shortAnswers);

		String json = objectMapper.writeValueAsString(request);

		// Mock 반환값 설정 - 주관식 DTO 반환
		com.coniv.mait.domain.question.service.dto.ShortQuestionDto mockResponse =
			com.coniv.mait.domain.question.service.dto.ShortQuestionDto.builder()
				.id(questionId)
				.content("객관식에서 주관식으로 변경된 문제")
				.explanation("변경된 해설")
				.number(1L)
				.shortAnswers(shortAnswers)
				.build();

		when(questionService.updateQuestion(eq(questionSetId), eq(questionId), any())).thenReturn(mockResponse);

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/{questionId}", questionSetId, questionId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.content").value("객관식에서 주관식으로 변경된 문제"),
				jsonPath("$.data.explanation").value("변경된 해설")
			);

		verify(questionService).updateQuestion(eq(questionSetId), eq(questionId), any());
	}

	@Test
	@DisplayName("문제 단건 삭제 API 테스트")
	void deleteQuestionTest() throws Exception {
		// given
		Long questionSetId = 1L;
		Long questionId = 1L;

		doNothing().when(questionService).deleteQuestion(questionSetId, questionId);

		// when & then
		mockMvc.perform(
				delete("/api/v1/question-sets/{questionSetId}/questions/{questionId}", questionSetId, questionId)
					.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		verify(questionService).deleteQuestion(questionSetId, questionId);
	}

	@Test
	@DisplayName("기본 문제 생성 API 테스트 - 성공")
	void createDefaultQuestion() throws Exception {
		// given
		final Long questionSetId = 1L;
		CreateDefaultQuestionApiRequest request = new CreateDefaultQuestionApiRequest(1L);
		MultipleQuestionDto questionDto = mock(MultipleQuestionDto.class);
		when(questionDto.getNumber()).thenReturn(1L);
		when(questionService.createDefaultQuestion(questionSetId)).thenReturn(questionDto);

		// when & then
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions/default", questionSetId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.number").value(1L)
			);

		verify(questionService).createDefaultQuestion(eq(questionSetId));
	}

	@Test
	@DisplayName("문제 순서 변경 API 테스트 - 성공")
	void updateQuestionOrderTest() throws Exception {
		// given
		final Long questionSetId = 1L;
		final Long sourceQuestionId = 2L;
		final Long prevQuestionId = 3L;
		final Long nextQuestionId = 4L;
		UpdateQuestionOrderApiRequest request = new UpdateQuestionOrderApiRequest(prevQuestionId, nextQuestionId);

		// when & then
		mockMvc.perform(
				patch("/api/v1/question-sets/{questionSetId}/questions/{questionId}/orders", questionSetId,
					sourceQuestionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk());

		verify(questionService).changeQuestionOrder(questionSetId, sourceQuestionId, prevQuestionId, nextQuestionId);
	}

	@Test
	@DisplayName("문제 순서 변경 API 테스트 - prevQuestionId와 nextQuestionId가 모두 null인 경우")
	void updateQuestionOrderBothNullTest() throws Exception {
		// given
		final Long questionSetId = 1L;
		final Long sourceQuestionId = 2L;
		UpdateQuestionOrderApiRequest request = new UpdateQuestionOrderApiRequest(null, null);

		// when & then
		mockMvc.perform(
				patch("/api/v1/question-sets/{questionSetId}/questions/{questionId}/orders", questionSetId,
					sourceQuestionId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(
				status().isBadRequest(),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.code").value("C-003"),
				jsonPath("$.reasons").isArray(),
				jsonPath("$.reasons[0]").value("이전 문제 ID와 다음 문제 ID가 모두 null일 수 없습니다.")
			);

		verify(questionService, never()).changeQuestionOrder(anyLong(), anyLong(), any(), any());
	}
}
