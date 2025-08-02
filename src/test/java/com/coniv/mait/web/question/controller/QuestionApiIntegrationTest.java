package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.OrderingQuestionOptionRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.question.dto.CreateFillBlankQuestionApiRequest;
import com.coniv.mait.web.question.dto.CreateMultipleQuestionApiRequest;
import com.coniv.mait.web.question.dto.CreateOrderingQuestionApiRequest;
import com.coniv.mait.web.question.dto.CreateShortQuestionApiRequest;

public class QuestionApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Autowired
	private ShortAnswerEntityRepository shortAnswerEntityRepository;

	@Autowired
	private OrderingQuestionOptionRepository orderingQuestionOptionRepository;

	@Autowired
	private FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@BeforeEach
	void setUp() {
		shortAnswerEntityRepository.deleteAll();
		multipleChoiceEntityRepository.deleteAll();
		orderingQuestionOptionRepository.deleteAll();
		fillBlankAnswerEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("문제 셋에 객관식 문제 저장 API 성공 테스트")
	void createMultipleQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		String questionContent = "Sample Question";
		String questionExplanation = "Sample Explanation";
		Long questionNumber = 1L;

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
				.build(),
			MultipleChoiceDto.builder()
				.number(3)
				.content("선택지 3")
				.isCorrect(false)
				.build()
		);

		CreateMultipleQuestionApiRequest request = new CreateMultipleQuestionApiRequest();
		request.setContent(questionContent);
		request.setExplanation(questionExplanation);
		request.setNumber(questionNumber);
		request.setChoices(choices);

		String json = objectMapper.writeValueAsString(request);
		json = json.replaceFirst("\\{", "{\"type\":\"MULTIPLE\",");

		// when
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type=MULTIPLE", savedQuestionSet.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		// then
		List<MultipleQuestionEntity> questions = questionEntityRepository.findAll()
			.stream()
			.filter(q -> q instanceof MultipleQuestionEntity)
			.map(q -> (MultipleQuestionEntity)q)
			.toList();

		assertThat(questions).hasSize(1);

		MultipleQuestionEntity savedQuestion = questions.getFirst();
		assertThat(savedQuestion.getContent()).isEqualTo(questionContent);
		assertThat(savedQuestion.getExplanation()).isEqualTo(questionExplanation);
		assertThat(savedQuestion.getNumber()).isEqualTo(questionNumber);
		assertThat(savedQuestion.getQuestionSet().getId()).isEqualTo(savedQuestionSet.getId());
		assertThat(savedQuestion.getAnswerCount()).isEqualTo(1); // 정답 개수

		List<MultipleChoiceEntity> savedChoices = multipleChoiceEntityRepository.findAll();
		assertThat(savedChoices).hasSize(3);
		assertThat(savedChoices).extracting("content")
			.containsExactlyInAnyOrder("선택지 1", "선택지 2", "선택지 3");
		assertThat(savedChoices).extracting("number")
			.containsExactlyInAnyOrder(1, 2, 3);

		long correctCount = savedChoices.stream()
			.mapToLong(choice -> choice.isCorrect() ? 1 : 0)
			.sum();
		assertThat(correctCount).isEqualTo(1);

		assertThat(savedChoices).allMatch(choice -> choice.getQuestion().getId().equals(savedQuestion.getId()));
	}

	@Test
	@DisplayName("문제 셋에 주관식 문제 저장 API 성공 테스트")
	void createShortQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		String questionContent = "주관식 문제 내용";
		String questionExplanation = "주관식 문제 해설";
		Long questionNumber = 1L;

		List<ShortAnswerDto> shortAnswers = List.of(
			ShortAnswerDto.builder()
				.answer("정답1")
				.isMain(true)
				.number(1L)
				.build(),
			ShortAnswerDto.builder()
				.answer("정답2")
				.isMain(false)
				.number(2L)
				.build(),
			ShortAnswerDto.builder()
				.answer("정답3")
				.isMain(false)
				.number(3L)
				.build()
		);

		CreateShortQuestionApiRequest request = new CreateShortQuestionApiRequest();
		request.setContent(questionContent);
		request.setExplanation(questionExplanation);
		request.setNumber(questionNumber);
		request.setShortAnswers(shortAnswers);

		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type=SHORT", savedQuestionSet.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		// then
		List<ShortQuestionEntity> questions = questionEntityRepository.findAll()
			.stream()
			.filter(q -> q instanceof ShortQuestionEntity)
			.map(q -> (ShortQuestionEntity)q)
			.toList();

		assertThat(questions).hasSize(1);

		ShortQuestionEntity savedQuestion = questions.getFirst();
		assertThat(savedQuestion.getContent()).isEqualTo(questionContent);
		assertThat(savedQuestion.getExplanation()).isEqualTo(questionExplanation);
		assertThat(savedQuestion.getNumber()).isEqualTo(questionNumber);
		assertThat(savedQuestion.getQuestionSet().getId()).isEqualTo(savedQuestionSet.getId());
		assertThat(savedQuestion.getAnswerCount()).isEqualTo(3); // 정답 개수

		List<ShortAnswerEntity> savedAnswers = shortAnswerEntityRepository.findAll();
		assertThat(savedAnswers).hasSize(3);
		assertThat(savedAnswers).extracting("answer")
			.containsExactlyInAnyOrder("정답1", "정답2", "정답3");
		assertThat(savedAnswers).extracting("number")
			.containsExactlyInAnyOrder(1L, 2L, 3L);

		long mainAnswerCount = savedAnswers.stream()
			.mapToLong(answer -> answer.isMain() ? 1 : 0)
			.sum();
		assertThat(mainAnswerCount).isEqualTo(1);

		assertThat(savedAnswers).allMatch(answer -> answer.getShortQuestionId().equals(savedQuestion.getId()));
	}

	@Test
	@DisplayName("문제 셋에 순서배열 문제 저장 API 성공 테스트")
	void createOrderingQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		String questionContent = "순서배열 문제 내용";
		String questionExplanation = "순서배열 문제 해설";
		Long questionNumber = 1L;

		List<OrderingQuestionOptionDto> options = List.of(
			OrderingQuestionOptionDto.builder()
				.content("첫 번째 단계")
				.originOrder(1)
				.answerOrder(3)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("두 번째 단계")
				.originOrder(2)
				.answerOrder(1)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("세 번째 단계")
				.originOrder(3)
				.answerOrder(2)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("네 번째 단계")
				.originOrder(4)
				.answerOrder(4)
				.build()
		);

		CreateOrderingQuestionApiRequest request = new CreateOrderingQuestionApiRequest();
		request.setContent(questionContent);
		request.setExplanation(questionExplanation);
		request.setNumber(questionNumber);
		request.setOptions(options);

		String json = objectMapper.writeValueAsString(request);
		// JSON에 type 필드가 없다면 추가
		if (!json.contains("\"type\"")) {
			json = json.replaceFirst("\\{", "{\"type\":\"ORDERING\",");
		}

		// when
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions?type=ORDERING", savedQuestionSet.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		// then
		List<OrderingQuestionEntity> questions = questionEntityRepository.findAll()
			.stream()
			.filter(q -> q instanceof OrderingQuestionEntity)
			.map(q -> (OrderingQuestionEntity)q)
			.toList();

		assertThat(questions).hasSize(1);

		OrderingQuestionEntity savedQuestion = questions.getFirst();
		assertThat(savedQuestion.getContent()).isEqualTo(questionContent);
		assertThat(savedQuestion.getExplanation()).isEqualTo(questionExplanation);
		assertThat(savedQuestion.getNumber()).isEqualTo(questionNumber);
		assertThat(savedQuestion.getQuestionSet().getId()).isEqualTo(savedQuestionSet.getId());

		List<OrderingOptionEntity> savedOptions = orderingQuestionOptionRepository.findAll();
		assertThat(savedOptions).hasSize(4);
		assertThat(savedOptions).extracting("content")
			.containsExactlyInAnyOrder("첫 번째 단계", "두 번째 단계", "세 번째 단계", "네 번째 단계");
		assertThat(savedOptions).extracting("originOrder")
			.containsExactlyInAnyOrder(1, 2, 3, 4);
		assertThat(savedOptions).extracting("answerOrder")
			.containsExactlyInAnyOrder(3, 1, 2, 4);

		assertThat(savedOptions).allMatch(option -> option.getOrderingQuestionId().equals(savedQuestion.getId()));
	}

	@SuppressWarnings("checkstyle:LineLength")
	@Test
	@DisplayName("문제 셋에 빈칸 문제 저장 API 성공 테스트")
	void createFillBlankQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		String questionContent = "빈칸에 들어갈 적절한 단어는 ___와 ___입니다.";
		String questionExplanation = "빈칸 문제 해설";
		Long questionNumber = 1L;

		List<FillBlankAnswerDto> fillBlankAnswers = List.of(
			FillBlankAnswerDto.builder()
				.answer("정답1")
				.isMain(true)
				.number(1L)
				.build(),
			FillBlankAnswerDto.builder()
				.answer("정답1_대안")
				.isMain(false)
				.number(1L)
				.build(),
			FillBlankAnswerDto.builder()
				.answer("정답2")
				.isMain(true)
				.number(2L)
				.build(),
			FillBlankAnswerDto.builder()
				.answer("정답2_대안")
				.isMain(false)
				.number(2L)
				.build()
		);

		CreateFillBlankQuestionApiRequest request = new CreateFillBlankQuestionApiRequest();
		request.setContent(questionContent);
		request.setExplanation(questionExplanation);
		request.setNumber(questionNumber);
		request.setFillBlankAnswers(fillBlankAnswers);

		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(
				post("/api/v1/question-sets/{questionSetId}/questions?type=FILL_BLANK", savedQuestionSet.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		// then
		List<FillBlankQuestionEntity> questions = questionEntityRepository.findAll()
			.stream()
			.filter(q -> q instanceof FillBlankQuestionEntity)
			.map(q -> (FillBlankQuestionEntity)q)
			.toList();

		assertThat(questions).hasSize(1);

		FillBlankQuestionEntity savedQuestion = questions.getFirst();
		assertThat(savedQuestion.getContent()).isEqualTo(questionContent);
		assertThat(savedQuestion.getExplanation()).isEqualTo(questionExplanation);
		assertThat(savedQuestion.getNumber()).isEqualTo(questionNumber);
		assertThat(savedQuestion.getQuestionSet().getId()).isEqualTo(savedQuestionSet.getId());

		List<FillBlankAnswerEntity> savedAnswers = fillBlankAnswerEntityRepository.findAll();
		assertThat(savedAnswers).hasSize(4);
		assertThat(savedAnswers).extracting("answer")
			.containsExactlyInAnyOrder("정답1", "정답1_대안", "정답2", "정답2_대안");
		assertThat(savedAnswers).extracting("number")
			.containsExactlyInAnyOrder(1L, 1L, 2L, 2L);

		// 각 번호별로 메인 답변이 하나씩 있는지 확인
		long mainAnswerCount = savedAnswers.stream()
			.mapToLong(answer -> answer.isMain() ? 1 : 0)
			.sum();
		assertThat(mainAnswerCount).isEqualTo(2); // 번호 1, 2 각각에 메인 답변 하나씩

		// 번호 1의 메인 답변 확인
		List<FillBlankAnswerEntity> number1Answers = savedAnswers.stream()
			.filter(answer -> answer.getNumber().equals(1L))
			.toList();
		assertThat(number1Answers).hasSize(2);
		long number1MainCount = number1Answers.stream()
			.mapToLong(answer -> answer.isMain() ? 1 : 0)
			.sum();
		assertThat(number1MainCount).isEqualTo(1);

		// 번호 2의 메인 답변 확인
		List<FillBlankAnswerEntity> number2Answers = savedAnswers.stream()
			.filter(answer -> answer.getNumber().equals(2L))
			.toList();
		assertThat(number2Answers).hasSize(2);
		long number2MainCount = number2Answers.stream()
			.mapToLong(answer -> answer.isMain() ? 1 : 0)
			.sum();
		assertThat(number2MainCount).isEqualTo(1);

		assertThat(savedAnswers).allMatch(answer -> answer.getFillBlankQuestionId().equals(savedQuestion.getId()));
	}

	@Test
	@DisplayName("객관식 문제 조회 API 성공 테스트")
	void getMultipleQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		MultipleQuestionEntity question = MultipleQuestionEntity.builder()
			.content("객관식 문제 내용")
			.explanation("객관식 문제 해설")
			.number(1L)
			.questionSet(savedQuestionSet)
			.build();
		MultipleQuestionEntity savedQuestion = questionEntityRepository.save(question);

		List<MultipleChoiceEntity> choices = List.of(
			MultipleChoiceEntity.builder()
				.question(savedQuestion)
				.number(1)
				.content("선택지 1")
				.isCorrect(true)
				.build(),
			MultipleChoiceEntity.builder()
				.question(savedQuestion)
				.number(2)
				.content("선택지 2")
				.isCorrect(false)
				.build()
		);
		multipleChoiceEntityRepository.saveAll(choices);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				savedQuestionSet.getId(), savedQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.id").value(savedQuestion.getId()),
				jsonPath("$.data.content").value("객관식 문제 내용"),
				jsonPath("$.data.explanation").value("객관식 문제 해설"),
				jsonPath("$.data.number").value(1),
				jsonPath("$.data.choices").isArray(),
				jsonPath("$.data.choices.length()").value(2),
				jsonPath("$.data.choices[0].number").value(1),
				jsonPath("$.data.choices[0].content").value("선택지 1"),
				jsonPath("$.data.choices[0].isCorrect").value(true),
				jsonPath("$.data.choices[1].number").value(2),
				jsonPath("$.data.choices[1].content").value("선택지 2"),
				jsonPath("$.data.choices[1].isCorrect").value(false)
			);
	}

	@Test
	@DisplayName("주관식 문제 조회 API 성공 테스트")
	void getShortQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		ShortQuestionEntity question = ShortQuestionEntity.builder()
			.content("주관식 문제 내용")
			.explanation("주관식 문제 해설")
			.number(1L)
			.questionSet(savedQuestionSet)
			.build();
		ShortQuestionEntity savedQuestion = questionEntityRepository.save(question);

		List<ShortAnswerEntity> answers = List.of(
			ShortAnswerEntity.builder()
				.shortQuestionId(savedQuestion.getId())
				.answer("정답1")
				.isMain(true)
				.number(1L)
				.build(),
			ShortAnswerEntity.builder()
				.shortQuestionId(savedQuestion.getId())
				.answer("정답2")
				.isMain(false)
				.number(1L)
				.build()
		);
		shortAnswerEntityRepository.saveAll(answers);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				savedQuestionSet.getId(), savedQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.type").value("SHORT"),
				jsonPath("$.data.id").value(savedQuestion.getId()),
				jsonPath("$.data.content").value("주관식 문제 내용"),
				jsonPath("$.data.explanation").value("주관식 문제 해설"),
				jsonPath("$.data.number").value(1),
				jsonPath("$.data.answers").isArray(),
				jsonPath("$.data.answers.length()").value(2),
				jsonPath("$.data.answers[0].answer").value("정답1"),
				jsonPath("$.data.answers[0].isMain").value(true),
				jsonPath("$.data.answers[0].number").value(1),
				jsonPath("$.data.answers[1].answer").value("정답2"),
				jsonPath("$.data.answers[1].isMain").value(false),
				jsonPath("$.data.answers[1].number").value(1)
			);
	}

	@Test
	@DisplayName("순서배열 문제 조회 API 성공 테스트")
	void getOrderingQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		OrderingQuestionEntity question = OrderingQuestionEntity.builder()
			.content("순서배열 문제 내용")
			.explanation("순서배열 문제 해설")
			.number(1L)
			.questionSet(savedQuestionSet)
			.build();
		OrderingQuestionEntity savedQuestion = questionEntityRepository.save(question);

		List<OrderingOptionEntity> options = List.of(
			OrderingOptionEntity.builder()
				.orderingQuestionId(savedQuestion.getId())
				.content("첫 번째 단계")
				.originOrder(1)
				.answerOrder(2)
				.build(),
			OrderingOptionEntity.builder()
				.orderingQuestionId(savedQuestion.getId())
				.content("두 번째 단계")
				.originOrder(2)
				.answerOrder(1)
				.build()
		);
		orderingQuestionOptionRepository.saveAll(options);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				savedQuestionSet.getId(), savedQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.type").value("ORDERING"),
				jsonPath("$.data.id").value(savedQuestion.getId()),
				jsonPath("$.data.content").value("순서배열 문제 내용"),
				jsonPath("$.data.explanation").value("순서배열 문제 해설"),
				jsonPath("$.data.number").value(1),
				jsonPath("$.data.options").isArray(),
				jsonPath("$.data.options.length()").value(2),
				jsonPath("$.data.options[0].content").value("첫 번째 단계"),
				jsonPath("$.data.options[0].originOrder").value(1),
				jsonPath("$.data.options[0].answerOrder").value(2),
				jsonPath("$.data.options[1].content").value("두 번째 단계"),
				jsonPath("$.data.options[1].originOrder").value(2),
				jsonPath("$.data.options[1].answerOrder").value(1)
			);
	}

	@Test
	@DisplayName("빈칸 문제 조회 API 성공 테스트")
	void getFillBlankQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		FillBlankQuestionEntity question = FillBlankQuestionEntity.builder()
			.content("빈칸에 들어갈 적절한 단어는 ___입니다.")
			.explanation("빈칸 문제 해설")
			.number(1L)
			.questionSet(savedQuestionSet)
			.build();
		FillBlankQuestionEntity savedQuestion = questionEntityRepository.save(question);

		List<FillBlankAnswerEntity> answers = List.of(
			FillBlankAnswerEntity.builder()
				.fillBlankQuestionId(savedQuestion.getId())
				.answer("정답1")
				.isMain(true)
				.number(1L)
				.build(),
			FillBlankAnswerEntity.builder()
				.fillBlankQuestionId(savedQuestion.getId())
				.answer("정답2")
				.isMain(false)
				.number(1L)
				.build()
		);
		fillBlankAnswerEntityRepository.saveAll(answers);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				savedQuestionSet.getId(), savedQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.type").value("FILL_BLANK"),
				jsonPath("$.data.id").value(savedQuestion.getId()),
				jsonPath("$.data.content").value("빈칸에 들어갈 적절한 단어는 ___입니다."),
				jsonPath("$.data.explanation").value("빈칸 문제 해설"),
				jsonPath("$.data.number").value(1),
				jsonPath("$.data.answers").isArray(),
				jsonPath("$.data.answers.length()").value(2),
				jsonPath("$.data.answers[0].answer").value("정답1"),
				jsonPath("$.data.answers[0].isMain").value(true),
				jsonPath("$.data.answers[0].number").value(1),
				jsonPath("$.data.answers[1].answer").value("정답2"),
				jsonPath("$.data.answers[1].isMain").value(false),
				jsonPath("$.data.answers[1].number").value(1)
			);
	}
}
