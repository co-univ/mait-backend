package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.coniv.mait.domain.question.constant.QuestionConstant;
import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionType;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.question.service.dto.FillBlankAnswerDto;
import com.coniv.mait.domain.question.service.dto.MultipleChoiceDto;
import com.coniv.mait.domain.question.service.dto.OrderingQuestionOptionDto;
import com.coniv.mait.domain.question.service.dto.ShortAnswerDto;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.question.dto.UpdateFillBlankQuestionApiRequest;
import com.coniv.mait.web.question.dto.UpdateMultipleQuestionApiRequest;
import com.coniv.mait.web.question.dto.UpdateOrderingQuestionApiRequest;
import com.coniv.mait.web.question.dto.UpdateQuestionOrderApiRequest;
import com.coniv.mait.web.question.dto.UpdateShortQuestionApiRequest;

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
	private OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Autowired
	private FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@BeforeEach
	void setUp() {
		shortAnswerEntityRepository.deleteAll();
		multipleChoiceEntityRepository.deleteAll();
		orderingOptionEntityRepository.deleteAll();
		fillBlankAnswerEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
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
			.lexoRank("m")
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
				jsonPath("$.data.type").value(QuestionType.MULTIPLE.name()),
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
			.lexoRank("m")
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
				jsonPath("$.data.type").value(QuestionType.SHORT.name()),
				jsonPath("$.data.id").value(savedQuestion.getId()),
				jsonPath("$.data.content").value("주관식 문제 내용"),
				jsonPath("$.data.explanation").value("주관식 문제 해설"),
				jsonPath("$.data.number").value(1),
				jsonPath("$.data.answerCount").value(2),
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
			.lexoRank("m")
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
		orderingOptionEntityRepository.saveAll(options);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				savedQuestionSet.getId(), savedQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.type").value(QuestionType.ORDERING.name()),
				jsonPath("$.data.id").value(savedQuestion.getId()),
				jsonPath("$.data.content").value("순서배열 문제 내용"),
				jsonPath("$.data.explanation").value("순서배열 문제 해설"),
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
			.lexoRank("m")
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
				jsonPath("$.data.type").value(QuestionType.FILL_BLANK.name()),
				jsonPath("$.data.id").value(savedQuestion.getId()),
				jsonPath("$.data.content").value("빈칸에 들어갈 적절한 단어는 ___입니다."),
				jsonPath("$.data.explanation").value("빈칸 문제 해설"),
				jsonPath("$.data.number").value(1),
				jsonPath("$.data.blankCount").value(1),
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
	@DisplayName("문제 셋의 모든 문제 조회 API 성공 테스트 - 다양한 타입")
	void getQuestionsApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		// 객관식 문제 생성 (number: 2)
		MultipleQuestionEntity multipleQuestion = MultipleQuestionEntity.builder()
			.content("객관식 문제")
			.explanation("객관식 문제 해설")
			.number(2L)
			.lexoRank("2")
			.questionSet(savedQuestionSet)
			.build();
		MultipleQuestionEntity savedMultipleQuestion = questionEntityRepository.save(multipleQuestion);

		List<MultipleChoiceEntity> choices = List.of(
			MultipleChoiceEntity.builder()
				.question(savedMultipleQuestion)
				.number(1)
				.content("선택지 1")
				.isCorrect(true)
				.build(),
			MultipleChoiceEntity.builder()
				.question(savedMultipleQuestion)
				.number(2)
				.content("선택지 2")
				.isCorrect(false)
				.build()
		);
		multipleChoiceEntityRepository.saveAll(choices);

		// 주관식 문제 생성 (number: 1)
		ShortQuestionEntity shortQuestion = ShortQuestionEntity.builder()
			.content("주관식 문제")
			.explanation("주관식 문제 해설")
			.lexoRank("1")
			.number(1L)
			.questionSet(savedQuestionSet)
			.build();
		ShortQuestionEntity savedShortQuestion = questionEntityRepository.save(shortQuestion);

		List<ShortAnswerEntity> shortAnswers = List.of(
			ShortAnswerEntity.builder()
				.shortQuestionId(savedShortQuestion.getId())
				.answer("주관식 정답")
				.isMain(true)
				.number(1L)
				.build()
		);
		shortAnswerEntityRepository.saveAll(shortAnswers);

		// 순서배열 문제 생성 (number: 3)
		OrderingQuestionEntity orderingQuestion = OrderingQuestionEntity.builder()
			.content("순서배열 문제")
			.explanation("순서배열 문제 해설")
			.number(3L)
			.lexoRank("3")
			.questionSet(savedQuestionSet)
			.build();
		OrderingQuestionEntity savedOrderingQuestion = questionEntityRepository.save(orderingQuestion);

		List<OrderingOptionEntity> options = List.of(
			OrderingOptionEntity.builder()
				.orderingQuestionId(savedOrderingQuestion.getId())
				.content("첫 번째 단계")
				.originOrder(1)
				.answerOrder(1)
				.build(),
			OrderingOptionEntity.builder()
				.orderingQuestionId(savedOrderingQuestion.getId())
				.content("두 번째 단계")
				.originOrder(2)
				.answerOrder(2)
				.build()
		);
		orderingOptionEntityRepository.saveAll(options);

		// 빈칸 문제 생성 (number: 4)
		FillBlankQuestionEntity fillBlankQuestion = FillBlankQuestionEntity.builder()
			.content("빈칸 문제")
			.explanation("빈칸 문제 해설")
			.number(4L)
			.lexoRank("4")
			.questionSet(savedQuestionSet)
			.build();
		FillBlankQuestionEntity savedFillBlankQuestion = questionEntityRepository.save(fillBlankQuestion);

		List<FillBlankAnswerEntity> fillBlankAnswers = List.of(
			FillBlankAnswerEntity.builder()
				.fillBlankQuestionId(savedFillBlankQuestion.getId())
				.answer("빈칸 정답")
				.isMain(true)
				.number(1L)
				.build()
		);
		fillBlankAnswerEntityRepository.saveAll(fillBlankAnswers);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions", savedQuestionSet.getId())
				.param("mode", "MAKING")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").isArray(),
				jsonPath("$.data.length()").value(4),
				jsonPath("$.data[0].content").value("주관식 문제"),
				jsonPath("$.data[0].type").value(QuestionType.SHORT.name()),
				jsonPath("$.data[1].content").value("객관식 문제"),
				jsonPath("$.data[1].type").value(QuestionType.MULTIPLE.name()),
				jsonPath("$.data[2].content").value("순서배열 문제"),
				jsonPath("$.data[2].type").value(QuestionType.ORDERING.name()),
				jsonPath("$.data[3].content").value("빈칸 문제"),
				jsonPath("$.data[3].type").value(QuestionType.FILL_BLANK.name())
			);
	}

	@Test
	@DisplayName("단답형 문제 수정 API 성공 테스트")
	void updateShortQuestionApiSuccess() throws Exception {
		// given - 문제셋과 기존 문제 생성
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		ShortQuestionEntity originalQuestion = ShortQuestionEntity.builder()
			.content("원본 주관식 문제 내용")
			.explanation("원본 문제 해설")
			.number(1L)
			.questionSet(savedQuestionSet)
			.lexoRank("1234")
			.build();
		ShortQuestionEntity savedQuestion = questionEntityRepository.save(originalQuestion);

		List<ShortAnswerEntity> originalAnswers = List.of(
			ShortAnswerEntity.builder()
				.shortQuestionId(savedQuestion.getId())
				.answer("원본 정답1")
				.isMain(true)
				.number(1L)
				.build()
		);
		shortAnswerEntityRepository.saveAll(originalAnswers);

		// 수정할 데이터 준비 - 같은 number에 main과 일반 답안 설정
		List<ShortAnswerDto> updatedAnswers = List.of(
			ShortAnswerDto.builder()
				.answer("수정된 정답1")
				.main(true)
				.number(1L)
				.build(),
			ShortAnswerDto.builder()
				.answer("수정된 정답2")
				.main(false)
				.number(1L)  // number 1로 변경하여 같은 번호에 main/일반 답안 구성
				.build()
		);

		UpdateShortQuestionApiRequest request = new UpdateShortQuestionApiRequest();
		request.setId(savedQuestion.getId());
		request.setContent("수정된 주관식 문제 내용");
		request.setExplanation("수정된 문제 해설");
		request.setNumber(1L);
		request.setShortAnswers(updatedAnswers);

		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				savedQuestionSet.getId(), savedQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpectAll(status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").exists(),
				jsonPath("$.data.content").value("수정된 주관식 문제 내용"),
				jsonPath("$.data.explanation").value("수정된 문제 해설")
			);

		// then - 수정된 문제 확인
		ShortQuestionEntity updatedQuestion = (ShortQuestionEntity)questionEntityRepository
			.findById(savedQuestion.getId()).orElseThrow();

		assertThat(updatedQuestion.getContent()).isEqualTo("수정된 주관식 문제 내용");
		assertThat(updatedQuestion.getExplanation()).isEqualTo("수정된 문제 해설");
		assertThat(updatedQuestion.getNumber()).isEqualTo(1L);

		List<ShortAnswerEntity> updatedAnswerEntities = shortAnswerEntityRepository.findAll();
		assertThat(updatedAnswerEntities).hasSize(2);
		assertThat(updatedAnswerEntities).extracting("answer")
			.containsExactlyInAnyOrder("수정된 정답1", "수정된 정답2");
	}

	@Test
	@DisplayName("객관식 문제 수정 API 성공 테스트")
	void updateMultipleQuestionApiSuccess() throws Exception {
		// given - 문제셋과 기존 문제 생성
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		MultipleQuestionEntity originalQuestion = MultipleQuestionEntity.builder()
			.content("원본 객관식 문제 내용")
			.explanation("원본 문제 해설")
			.number(1L)
			.questionSet(savedQuestionSet)
			.lexoRank("1234")
			.build();
		MultipleQuestionEntity savedQuestion = questionEntityRepository.save(originalQuestion);

		List<MultipleChoiceEntity> originalChoices = List.of(
			MultipleChoiceEntity.builder()
				.question(savedQuestion)
				.number(1)
				.content("원본 선택지 1")
				.isCorrect(true)
				.build(),
			MultipleChoiceEntity.builder()
				.question(savedQuestion)
				.number(2)
				.content("원본 선택지 2")
				.isCorrect(false)
				.build()
		);
		multipleChoiceEntityRepository.saveAll(originalChoices);

		// 수정할 데이터 준비
		List<MultipleChoiceDto> updatedChoices = List.of(
			MultipleChoiceDto.builder()
				.number(1)
				.content("수정된 선택지 1")
				.isCorrect(false)
				.build(),
			MultipleChoiceDto.builder()
				.number(2)
				.content("수정된 선택지 2")
				.isCorrect(true)
				.build(),
			MultipleChoiceDto.builder()
				.number(3)
				.content("새로운 선택지 3")
				.isCorrect(false)
				.build()
		);

		UpdateMultipleQuestionApiRequest request = new UpdateMultipleQuestionApiRequest();
		request.setId(savedQuestion.getId());
		request.setContent("수정된 객관식 문제 내용");
		request.setExplanation("수정된 문제 해설");
		request.setNumber(1L);
		request.setChoices(updatedChoices);

		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				savedQuestionSet.getId(), savedQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.content").value("수정된 객관식 문제 내용"))
			.andExpect(jsonPath("$.data.explanation").value("수정된 문제 해설"));

		// then - 수정된 문제 확인
		MultipleQuestionEntity updatedQuestion = (MultipleQuestionEntity)questionEntityRepository
			.findById(savedQuestion.getId()).orElseThrow();

		assertThat(updatedQuestion.getContent()).isEqualTo("수정된 객관식 문제 내용");
		assertThat(updatedQuestion.getExplanation()).isEqualTo("수정된 문제 해설");
		assertThat(updatedQuestion.getNumber()).isEqualTo(1L);

		List<MultipleChoiceEntity> updatedChoiceEntities = multipleChoiceEntityRepository.findAll();
		assertThat(updatedChoiceEntities).hasSize(3);
		assertThat(updatedChoiceEntities).extracting("content")
			.containsExactlyInAnyOrder("수정된 선택지 1", "수정된 선택지 2", "새로운 선택지 3");

		// 정답이 2번으로 변경되었는지 확인
		long correctCount = updatedChoiceEntities.stream()
			.mapToLong(choice -> choice.isCorrect() ? 1 : 0)
			.sum();
		assertThat(correctCount).isEqualTo(1);

		MultipleChoiceEntity correctChoice = updatedChoiceEntities.stream()
			.filter(MultipleChoiceEntity::isCorrect)
			.findFirst().orElseThrow();
		assertThat(correctChoice.getNumber()).isEqualTo(2);
		assertThat(correctChoice.getContent()).isEqualTo("수정된 선택지 2");
	}

	@Test
	@DisplayName("순서맞추기 문제 수정 API 성공 테스트")
	void updateOrderingQuestionApiSuccess() throws Exception {
		// given - 문제셋과 기존 문제 생성
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		OrderingQuestionEntity originalQuestion = OrderingQuestionEntity.builder()
			.content("원본 순서맞추기 문제 내용")
			.explanation("원본 문제 해설")
			.number(1L)
			.lexoRank("1234")
			.questionSet(savedQuestionSet)
			.build();
		OrderingQuestionEntity savedQuestion = questionEntityRepository.save(originalQuestion);

		List<OrderingOptionEntity> originalOptions = List.of(
			OrderingOptionEntity.builder()
				.orderingQuestionId(savedQuestion.getId())
				.content("원본 첫 번째 단계")
				.originOrder(1)
				.answerOrder(1)
				.build(),
			OrderingOptionEntity.builder()
				.orderingQuestionId(savedQuestion.getId())
				.content("원본 두 번째 단계")
				.originOrder(2)
				.answerOrder(2)
				.build()
		);
		orderingOptionEntityRepository.saveAll(originalOptions);

		// 수정할 데이터 준비
		List<OrderingQuestionOptionDto> updatedOptions = List.of(
			OrderingQuestionOptionDto.builder()
				.content("수정된 첫 번째 단계")
				.originOrder(1)
				.answerOrder(3)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("수정된 두 번째 단계")
				.originOrder(2)
				.answerOrder(1)
				.build(),
			OrderingQuestionOptionDto.builder()
				.content("새로운 세 번째 단계")
				.originOrder(3)
				.answerOrder(2)
				.build()
		);

		UpdateOrderingQuestionApiRequest request = new UpdateOrderingQuestionApiRequest();
		request.setId(savedQuestion.getId());
		request.setContent("수정된 순서맞추기 문제 내용");
		request.setExplanation("수정된 문제 해설");
		request.setNumber(1L);
		request.setOptions(updatedOptions);

		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				savedQuestionSet.getId(), savedQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.content").value("수정된 순서맞추기 문제 내용"))
			.andExpect(jsonPath("$.data.explanation").value("수정된 문제 해설"));

		// then - 수정된 문제 확인
		OrderingQuestionEntity updatedQuestion = (OrderingQuestionEntity)questionEntityRepository
			.findById(savedQuestion.getId()).orElseThrow();

		assertThat(updatedQuestion.getContent()).isEqualTo("수정된 순서맞추기 문제 내용");
		assertThat(updatedQuestion.getExplanation()).isEqualTo("수정된 문제 해설");
		assertThat(updatedQuestion.getNumber()).isEqualTo(1L);

		List<OrderingOptionEntity> updatedOptionEntities = orderingOptionEntityRepository.findAll();
		assertThat(updatedOptionEntities).hasSize(3);
		assertThat(updatedOptionEntities).extracting("content")
			.containsExactlyInAnyOrder("수정된 첫 번째 단계", "수정된 두 번째 단계", "새로운 세 번째 단계");
		assertThat(updatedOptionEntities).extracting("originOrder")
			.containsExactlyInAnyOrder(1, 2, 3);
		assertThat(updatedOptionEntities).extracting("answerOrder")
			.containsExactlyInAnyOrder(3, 1, 2);
	}

	@Test
	@DisplayName("빈칸채우기 문제 수정 API 성공 테스트")
	void updateFillBlankQuestionApiSuccess() throws Exception {
		// given - 문제셋과 기존 문제 생성
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		FillBlankQuestionEntity originalQuestion = FillBlankQuestionEntity.builder()
			.content("원본 빈칸에 들어갈 적절한 단어는 ___입니다.")
			.explanation("원본 문제 해설")
			.number(1L)
			.lexoRank("1234")
			.questionSet(savedQuestionSet)
			.build();
		FillBlankQuestionEntity savedQuestion = questionEntityRepository.save(originalQuestion);

		List<FillBlankAnswerEntity> originalAnswers = List.of(
			FillBlankAnswerEntity.builder()
				.fillBlankQuestionId(savedQuestion.getId())
				.answer("원본정답1")
				.isMain(true)
				.number(1L)
				.build()
		);
		fillBlankAnswerEntityRepository.saveAll(originalAnswers);

		// 수정할 데이터 준비
		List<FillBlankAnswerDto> updatedAnswers = List.of(
			FillBlankAnswerDto.builder()
				.answer("수정된정답1")
				.main(true)
				.number(1L)
				.build(),
			FillBlankAnswerDto.builder()
				.answer("수정된정답1_대안")
				.main(false)
				.number(1L)
				.build(),
			FillBlankAnswerDto.builder()
				.answer("수정된정답2")
				.main(true)
				.number(2L)
				.build()
		);

		UpdateFillBlankQuestionApiRequest request = new UpdateFillBlankQuestionApiRequest();
		request.setId(savedQuestion.getId());
		request.setContent("수정된 빈칸에 들어갈 적절한 단어는 ___와 ___입니다.");
		request.setExplanation("수정된 문제 해설");
		request.setNumber(1L);
		request.setFillBlankAnswers(updatedAnswers);

		String json = objectMapper.writeValueAsString(request);

		// when
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				savedQuestionSet.getId(), savedQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").exists())
			.andExpect(jsonPath("$.data.content").value("수정된 빈칸에 들어갈 적절한 단어는 ___와 ___입니다."))
			.andExpect(jsonPath("$.data.explanation").value("수정된 문제 해설"));

		// then - 수정된 문제 확인
		FillBlankQuestionEntity updatedQuestion = (FillBlankQuestionEntity)questionEntityRepository
			.findById(savedQuestion.getId()).orElseThrow();

		assertThat(updatedQuestion.getContent()).isEqualTo("수정된 빈칸에 들어갈 적절한 단어는 ___와 ___입니다.");
		assertThat(updatedQuestion.getExplanation()).isEqualTo("수정된 문제 해설");
		assertThat(updatedQuestion.getNumber()).isEqualTo(1L);

		List<FillBlankAnswerEntity> updatedAnswerEntities = fillBlankAnswerEntityRepository.findAll();
		assertThat(updatedAnswerEntities).hasSize(3);
		assertThat(updatedAnswerEntities).extracting("answer")
			.containsExactlyInAnyOrder("수정된정답1", "수정된정답1_대안", "수정된정답2");
		assertThat(updatedAnswerEntities).extracting("number")
			.containsExactlyInAnyOrder(1L, 1L, 2L);

		// 메인 답변이 각 번호별로 하나씩 있는지 확인
		long mainAnswerCount = updatedAnswerEntities.stream()
			.mapToLong(answer -> answer.isMain() ? 1 : 0)
			.sum();
		assertThat(mainAnswerCount).isEqualTo(2); // 번호 1, 2 각각에 메인 답변 하나씩
	}

	@Test
	@DisplayName("문제 단건 삭제 API 성공 테스트")
	void deleteQuestionApiSuccess() throws Exception {
		// given - 문제셋과 기존 문제 생성
		QuestionSetEntity questionSet = QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL);
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		MultipleQuestionEntity question = MultipleQuestionEntity.builder()
			.content("삭제할 객관식 문제 내용")
			.explanation("삭제할 문제 해설")
			.number(1L)
			.lexoRank("1234")
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

		// when
		mockMvc.perform(delete("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				savedQuestionSet.getId(), savedQuestion.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").doesNotExist());

		// then - 문제와 관련된 선택지도 함께 삭제되었는지 확인
		boolean questionExists = questionEntityRepository.existsById(savedQuestion.getId());
		assertThat(questionExists).isFalse();

		List<MultipleChoiceEntity> remainingChoices = multipleChoiceEntityRepository.findAll();
		assertThat(remainingChoices).isEmpty();
	}

	@Test
	@DisplayName("기본 문제 생성 API 통합 테스트")
	void createBasicQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL));

		// when
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions/default", questionSet.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").exists(),
				jsonPath("$.data.content").value(QuestionConstant.DEFAULT_QUESTION_CONTENT),
				jsonPath("$.data.type").value(QuestionType.MULTIPLE.name()));

		// then - DB에 문제 저장되었는지 확인
		List<QuestionEntity> questions = questionEntityRepository.findAll();
		assertThat(questions).hasSize(1);

		QuestionEntity savedQuestion = questions.get(0);
		assertThat(savedQuestion.getContent()).isEqualTo(QuestionConstant.DEFAULT_QUESTION_CONTENT);
		assertThat(savedQuestion.getQuestionSet().getId()).isEqualTo(questionSet.getId());
		assertThat(savedQuestion).isInstanceOf(MultipleQuestionEntity.class);
	}

	@Test
	@DisplayName("문제 순서 변경 API 통합 테스트")
	void updateQuestionOrderApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.of("Sample Subject", QuestionSetCreationType.MANUAL));

		QuestionEntity defaultQuestion = questionEntityRepository.save(
			QuestionEntity.createDefaultQuestion(questionSet, "123"));

		QuestionEntity prevQuestion = questionEntityRepository.save(
			QuestionEntity.createDefaultQuestion(questionSet, "1245"));

		QuestionEntity nextQuestion = questionEntityRepository.save(
			QuestionEntity.createDefaultQuestion(questionSet, "12451"));

		UpdateQuestionOrderApiRequest request = new UpdateQuestionOrderApiRequest(prevQuestion.getId(),
			nextQuestion.getId());

		// when
		mockMvc.perform(
				patch("/api/v1/question-sets/{questionSetId}/questions/{questionId}/orders", questionSet.getId(),
					defaultQuestion.getId())
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").doesNotExist());

		// then
		List<QuestionEntity> questions = questionEntityRepository.findAllByQuestionSetId(questionSet.getId()).stream()
			.sorted(Comparator.comparing(QuestionEntity::getLexoRank)).toList();

		assertThat(questions).hasSize(3);
		assertThat(questions.get(0).getId()).isEqualTo(prevQuestion.getId());
		assertThat(questions.get(1).getId()).isEqualTo(defaultQuestion.getId());
		assertThat(questions.get(2).getId()).isEqualTo(nextQuestion.getId());
	}
}
