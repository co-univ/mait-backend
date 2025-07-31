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

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.OrderingQuestionOptionRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.web.integration.BaseIntegrationTest;
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

	@BeforeEach
	void setUp() {
		shortAnswerEntityRepository.deleteAll();
		multipleChoiceEntityRepository.deleteAll();
		orderingQuestionOptionRepository.deleteAll();
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

		MultipleQuestionEntity savedQuestion = questions.get(0);
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

		ShortQuestionEntity savedQuestion = questions.get(0);
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

		OrderingQuestionEntity savedQuestion = questions.get(0);
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
}
