package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

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
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
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
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.question.dto.UpdateFillBlankQuestionApiRequest;
import com.coniv.mait.web.question.dto.UpdateMultipleQuestionApiRequest;
import com.coniv.mait.web.question.dto.UpdateOrderingQuestionApiRequest;
import com.coniv.mait.web.question.dto.UpdateQuestionOrderApiRequest;
import com.coniv.mait.web.question.dto.UpdateShortQuestionApiRequest;
import com.fasterxml.jackson.databind.JsonNode;

@WithCustomUser
public class QuestionApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private UserEntityRepository userEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private TeamUserEntityRepository teamUserEntityRepository;

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


	@Autowired
	private TransactionTemplate transactions;

	@Autowired
	private SolvingSessionEntityRepository sessions;

	private Fixture fixture;

	private Long otherUserId;

	@Test
	@DisplayName("기본 객관식 문제는 오답 선택지 4개와 정답 개수 0으로 생성된다")
	void createDefaultMultipleQuestionHasZeroAnswers() throws Exception {
		// given
		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			questionSetInMakerTeam("기본 문제 테스트"));

		// when
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions/default", questionSet.getId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data.answerCount").value(0),
				jsonPath("$.data.choices.length()").value(4)
			);

		// then
		MultipleQuestionEntity question = (MultipleQuestionEntity)questionEntityRepository
			.findAllByQuestionSetId(questionSet.getId()).get(0);
		assertThat(question.getAnswerCount()).isZero();
		assertThat(multipleChoiceEntityRepository.findAllByQuestionId(question.getId()))
			.hasSize(4)
			.allMatch(choice -> !choice.isCorrect());
	}

	@Test
	@DisplayName("객관식 문제 조회 API 성공 테스트")
	void getMultipleQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = questionSetInMakerTeam("Sample Title");
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		MultipleQuestionEntity question = MultipleQuestionEntity.builder()
			.content("객관식 문제 내용")
			.explanation("객관식 문제 해설")
			.answerCount(1)
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
				jsonPath("$.data.answerCount").value(1),
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
	@DisplayName("실시간 객관식 조회는 정답 여부를 숨기고 정답 개수를 반환한다")
	void getMultipleQuestionInLiveTimeReturnsAnswerCount() throws Exception {
		// given
		QuestionSetEntity teamQuestionSet = questionSetInMakerTeam("실시간 문제 셋");
		TeamUserEntity member = teamUserEntityRepository.findByTeamIdAndUserRole(teamQuestionSet.getTeamId(),
			TeamUserRole.MAKER).orElseThrow();
		member.updateUserRole(TeamUserRole.PLAYER);
		QuestionSetEntity questionSet = questionSetEntityRepository.save(QuestionSetEntity.builder()
			.teamId(teamQuestionSet.getTeamId())
			.title("실시간 문제 셋")
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.status(QuestionSetStatus.ONGOING)
			.build());
		MultipleQuestionEntity question = questionEntityRepository.save(MultipleQuestionEntity.builder()
			.content("정답이 두 개인 문제")
			.number(1L)
			.lexoRank("m")
			.questionSet(questionSet)
			.answerCount(2)
			.build());
		multipleChoiceEntityRepository.saveAll(List.of(
			MultipleChoiceEntity.builder().question(question).number(1).content("정답 1").isCorrect(true).build(),
			MultipleChoiceEntity.builder().question(question).number(2).content("오답").isCorrect(false).build(),
			MultipleChoiceEntity.builder().question(question).number(3).content("정답 2").isCorrect(true).build()
		));

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}/questions/{questionId}",
				questionSet.getId(), question.getId())
				.param("mode", "LIVE_TIME"))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.data.answerCount").value(2),
				jsonPath("$.data.choices.length()").value(3),
				jsonPath("$.data.choices[0].isCorrect").isEmpty(),
				jsonPath("$.data.choices[1].isCorrect").isEmpty(),
				jsonPath("$.data.choices[2].isCorrect").isEmpty()
			);
	}

	@Test
	@DisplayName("주관식 문제 조회 API 성공 테스트")
	void getShortQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = questionSetInMakerTeam("Sample Title");
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		ShortQuestionEntity question = ShortQuestionEntity.builder()
			.content("주관식 문제 내용")
			.explanation("주관식 문제 해설")
			.number(1L)
			.lexoRank("m")
			.questionSet(savedQuestionSet)
			.answerCount(1) // 정답 그룹(number) 개수 (= main 정답 개수)
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
				jsonPath("$.data.answerCount").value(1),
				jsonPath("$.data.answers").isArray(),
				jsonPath("$.data.answers.length()").value(2),
				jsonPath("$.data.answers[0].answer").value("정답1"),
				jsonPath("$.data.answers[0].main").value(true),
				jsonPath("$.data.answers[1].answer").value("정답2"),
				jsonPath("$.data.answers[1].main").value(false)
			);
	}

	@Test
	@DisplayName("순서배열 문제 조회 API 성공 테스트")
	void getOrderingQuestionApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = questionSetInMakerTeam("Sample Title");
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
		QuestionSetEntity questionSet = questionSetInMakerTeam("Sample Title");
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
				jsonPath("$.data.answers[0].main").value(true),
				jsonPath("$.data.answers[1].answer").value("정답2"),
				jsonPath("$.data.answers[1].main").value(false),
				jsonPath("$.data.answers[1].number").value(1)
			);
	}

	@Test
	@DisplayName("문제 셋의 모든 문제 조회 API 성공 테스트 - 다양한 타입")
	void getQuestionsApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = questionSetInMakerTeam("Sample Title");
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
		QuestionSetEntity questionSet = questionSetInMakerTeam("Sample Title");
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
		request.setAnswers(updatedAnswers);

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
		QuestionSetEntity questionSet = questionSetInMakerTeam("Sample Title");
		QuestionSetEntity savedQuestionSet = questionSetEntityRepository.save(questionSet);

		MultipleQuestionEntity originalQuestion = MultipleQuestionEntity.builder()
			.content("원본 객관식 문제 내용")
			.answerCount(1)
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
				.isCorrect(true)
				.build()
		);

		UpdateMultipleQuestionApiRequest request = new UpdateMultipleQuestionApiRequest();
		request.setId(savedQuestion.getId());
		request.setContent("수정된 객관식 문제 내용");
		request.setExplanation("수정된 문제 해설");
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
			.andExpect(jsonPath("$.data.answerCount").value(2))
			.andExpect(jsonPath("$.data.explanation").value("수정된 문제 해설"));

		// then - 수정된 문제 확인
		MultipleQuestionEntity updatedQuestion = (MultipleQuestionEntity)questionEntityRepository
			.findById(savedQuestion.getId()).orElseThrow();

		assertThat(updatedQuestion.getContent()).isEqualTo("수정된 객관식 문제 내용");
		assertThat(updatedQuestion.getAnswerCount()).isEqualTo(2);
		assertThat(updatedQuestion.getExplanation()).isEqualTo("수정된 문제 해설");
		assertThat(updatedQuestion.getNumber()).isEqualTo(1L);

		List<MultipleChoiceEntity> updatedChoiceEntities = multipleChoiceEntityRepository.findAll();
		assertThat(updatedChoiceEntities).hasSize(3);
		assertThat(updatedChoiceEntities).extracting("content")
			.containsExactlyInAnyOrder("수정된 선택지 1", "수정된 선택지 2", "새로운 선택지 3");

		// 정답이 1개에서 2개로 변경되었는지 확인
		long correctCount = updatedChoiceEntities.stream()
			.mapToLong(choice -> choice.isCorrect() ? 1 : 0)
			.sum();
		assertThat(correctCount).isEqualTo(updatedQuestion.getAnswerCount());
		assertThat(updatedChoiceEntities).filteredOn(MultipleChoiceEntity::isCorrect)
			.extracting(MultipleChoiceEntity::getNumber)
			.containsExactlyInAnyOrder(2, 3);
	}

	@Test
	@DisplayName("순서맞추기 문제 수정 API 성공 테스트")
	void updateOrderingQuestionApiSuccess() throws Exception {
		// given - 문제셋과 기존 문제 생성
		QuestionSetEntity questionSet = questionSetInMakerTeam("Sample Title");
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
		QuestionSetEntity questionSet = questionSetInMakerTeam("Sample Title");
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
		request.setAnswers(updatedAnswers);

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
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void deleteQuestionApiSuccess() throws Exception {
		// given - 문제셋과 기존 문제 생성
		QuestionSetEntity questionSet = questionSetInMakerTeam("Sample Title");
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
			questionSetInMakerTeam("Sample Title"));

		// when
		mockMvc.perform(post("/api/v1/question-sets/{questionSetId}/questions/default", questionSet.getId())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpectAll(status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data").exists(),
				jsonPath("$.data.type").value(QuestionType.MULTIPLE.name()));

		// then - DB에 문제 저장되었는지 확인
		List<QuestionEntity> questions = questionEntityRepository.findAll();
		assertThat(questions).hasSize(1);

		QuestionEntity savedQuestion = questions.get(0);
		assertThat(savedQuestion.getQuestionSet().getId()).isEqualTo(questionSet.getId());
		assertThat(savedQuestion).isInstanceOf(MultipleQuestionEntity.class);
	}

	@Test
	@DisplayName("문제 순서 변경 API 통합 테스트")
	void updateQuestionOrderApiSuccess() throws Exception {
		// given
		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			questionSetInMakerTeam("Sample Title"));

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

	private QuestionSetEntity questionSetInMakerTeam(String title) {
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("coniv", currentUser.getId()));
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(currentUser, team, TeamUserRole.MAKER));
		return QuestionSetEntity.builder()
			.title(title)
			.creationType(QuestionSetCreationType.MANUAL)
			.teamId(team.getId())
			.build();
	}

	@AfterEach
	void tearDown() {
		if (fixture == null) {
			return;
		}
		transactions.executeWithoutResult(status -> {
			sessions.deleteAllByQuestionSetId(fixture.questionSetId());
			multipleChoiceEntityRepository.deleteAllByQuestionIdIn(fixture.questionIds());
			shortAnswerEntityRepository.deleteAllByShortQuestionIdIn(fixture.questionIds());
			fillBlankAnswerEntityRepository.deleteAllByFillBlankQuestionIdIn(fixture.questionIds());
			orderingOptionEntityRepository.deleteAllByOrderingQuestionIdIn(fixture.questionIds());
			questionEntityRepository.deleteAllById(fixture.questionIds());
			questionSetEntityRepository.deleteById(fixture.questionSetId());
			teamUserEntityRepository.deleteAllById(fixture.membershipIds());
			teamEntityRepository.deleteById(fixture.teamId());
			if (otherUserId != null) {
				userEntityRepository.deleteById(otherUserId);
			}
		});
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@ParameterizedTest
	@MethodSource("playerRequests")
	@DisplayName("PLAYER의 풀이 조회는 정답을 숨기고 권한이나 상태에 맞지 않는 mode는 거절한다")
	void getQuestionsAndQuestion_playerCannotRevealAnswers(QuestionSetSolveMode solveMode, DeliveryMode mode)
		throws Exception {
		// given
		prepareFixture(solveMode, QuestionSetStatus.ONGOING, TeamUserRole.PLAYER);

		// when & then
		if (mode == null || mode == DeliveryMode.MAKING || mode == DeliveryMode.MANAGING) {
			assertForbidden(mode);
		} else if (mode == DeliveryMode.REVIEW) {
			assertRejected(mode, 400, "0002");
		} else if (mode == DeliveryMode.LIVE_TIME && solveMode != QuestionSetSolveMode.LIVE_TIME) {
			assertRejected(mode, 400, "2001");
		} else if (mode == DeliveryMode.STUDY && solveMode != QuestionSetSolveMode.STUDY) {
			assertRejected(mode, 400, "2002");
		} else {
			assertResponses(mode, false);
		}
	}

	static Stream<Arguments> playerRequests() {
		return Stream.of(QuestionSetSolveMode.values()).flatMap(solveMode ->
			Stream.concat(Stream.of((DeliveryMode)null), Stream.of(DeliveryMode.values()))
				.map(mode -> Arguments.of(solveMode, mode)));
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@ParameterizedTest
	@CsvSource({"STUDY, true, PROGRESSING, false", "STUDY, true, COMPLETE, true",
		"STUDY, false, COMPLETE, false", "LIVE_TIME, true, COMPLETE, false"})
	@DisplayName("학습모드에서는 본인의 채점 완료 세션만 정답 공개에 사용한다")
	void getQuestionsAndQuestion_ownCompletedStudySession(QuestionSetSolveMode solveMode, boolean own,
		SolvingStatus sessionStatus, boolean visible) throws Exception {
		// given
		prepareFixture(solveMode, QuestionSetStatus.ONGOING, TeamUserRole.PLAYER);
		Long requesterId = ((MaitUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
		transactions.executeWithoutResult(status -> {
			UserEntity user = own ? userEntityRepository.findById(requesterId).orElseThrow()
				: userEntityRepository.save(UserEntity.localLoginUser(
				"other-" + UUID.randomUUID() + "@test.com", "password", "다른 사용자", "other"));
			if (!own) {
				otherUserId = user.getId();
			}
			SolvingSessionEntity session = SolvingSessionEntity.studySession(user,
				questionSetEntityRepository.findById(fixture.questionSetId()).orElseThrow());
			if (sessionStatus == SolvingStatus.COMPLETE) {
				session.submit(4, 4);
			}
			sessions.save(session);
		});

		// when & then
		if (solveMode == QuestionSetSolveMode.STUDY) {
			assertResponses(DeliveryMode.STUDY, visible);
		} else {
			assertRejected(DeliveryMode.STUDY, 400, "2002");
		}
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@ParameterizedTest
	@CsvSource({"BEFORE", "AFTER"})
	@DisplayName("복습이 열리기 전에는 REVIEW 요청으로 정답을 공개할 수 없다")
	void getQuestionsAndQuestion_beforeReview(QuestionSetStatus status) throws Exception {
		// given
		prepareFixture(QuestionSetSolveMode.STUDY, status, TeamUserRole.PLAYER);

		// when & then
		assertRejected(DeliveryMode.REVIEW, 400, "0002");
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@ParameterizedTest
	@CsvSource({"MAKER, MAKING, STUDY, MAKING, true", "OWNER, MAKING, STUDY, MAKING, true",
		"MAKER, ONGOING, LIVE_TIME, MANAGING, true", "OWNER, ONGOING, LIVE_TIME, MANAGING, true",
		"MAKER, MAKING, STUDY, , true", "OWNER, MAKING, STUDY, , true",
		"MAKER, ONGOING, LIVE_TIME, LIVE_TIME, false", "OWNER, ONGOING, LIVE_TIME, LIVE_TIME, false",
		"MAKER, ONGOING, STUDY, STUDY, false", "OWNER, ONGOING, STUDY, STUDY, false",
		"PLAYER, REVIEW, STUDY, REVIEW, true", "MAKER, REVIEW, STUDY, REVIEW, true",
		"OWNER, REVIEW, LIVE_TIME, REVIEW, true"})
	@DisplayName("동일한 관리자도 편집 및 관리 화면에서는 정답을 보고 풀이 화면에서는 숨긴다")
	void getQuestionsAndQuestion_answerVisibilityByMode(TeamUserRole role, QuestionSetStatus status,
		QuestionSetSolveMode solveMode, DeliveryMode mode, boolean visible) throws Exception {
		// given
		prepareFixture(solveMode, status, role);

		// when & then
		assertResponses(mode, visible);
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@ParameterizedTest
	@MethodSource("allModes")
	@DisplayName("비멤버는 mode와 타인 ID를 바꿔도 단건과 목록 조회가 차단된다")
	void getQuestionsAndQuestion_nonMemberForbidden(DeliveryMode mode) throws Exception {
		// given
		prepareFixture(QuestionSetSolveMode.LIVE_TIME, QuestionSetStatus.ONGOING, null);

		// when & then
		assertForbidden(mode);
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@ParameterizedTest
	@MethodSource("allModes")
	@DisplayName("제작 중인 문제는 PLAYER가 어떤 mode로도 읽을 수 없다")
	void getQuestionsAndQuestion_draftPlayerForbidden(DeliveryMode mode) throws Exception {
		// given
		prepareFixture(QuestionSetSolveMode.STUDY, QuestionSetStatus.MAKING, TeamUserRole.PLAYER);

		// when & then
		assertForbidden(mode);
	}

	static Stream<Arguments> allModes() {
		return Stream.concat(Stream.of((DeliveryMode)null), Stream.of(DeliveryMode.values()))
			.map(Arguments::of);
	}

	private void assertForbidden(DeliveryMode mode) throws Exception {
		assertRejected(mode, 403, "C-008");
	}

	private void assertRejected(DeliveryMode mode, int expectedStatus, String code) throws Exception {
		mockMvc.perform(request(null, mode))
			.andExpectAll(status().is(expectedStatus), jsonPath("$.code").value(code));
		for (Long questionId : fixture.questionIds()) {
			mockMvc.perform(request(questionId, mode))
				.andExpectAll(status().is(expectedStatus), jsonPath("$.code").value(code));
		}
	}

	private void assertResponses(DeliveryMode mode, boolean visible) throws Exception {
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		JsonNode list = data(request(null, mode));
		assertThat(list.size()).isEqualTo(4);
		for (JsonNode question : list) {
			assertQuestion(question, visible);
			assertQuestion(data(request(question.get("id").asLong(), mode)), visible);
		}
	}

	private JsonNode data(MockHttpServletRequestBuilder request) throws Exception {
		String response = mockMvc.perform(request).andExpect(status().isOk()).andReturn().getResponse()
			.getContentAsString();
		return objectMapper.readTree(response).get("data");
	}

	private void assertQuestion(JsonNode question, boolean visible) {
		assertThat(question.path("content").asText()).isEqualTo("공개 문제");
		if (visible) {
			assertThat(question.path("explanation").asText()).isEqualTo("비공개 해설");
		} else {
			assertThat(question.path("explanation").isNull() || question.path("explanation").isMissingNode()).isTrue();
		}
		switch (question.path("type").asText()) {
			case "SHORT", "FILL_BLANK" -> {
				if (visible) {
					assertThat(question.path("answers").get(0).path("answer").asText()).isEqualTo("비공개 정답");
				} else {
					assertThat(question.path("answers").isNull() || question.path("answers").isMissingNode()).isTrue();
				}
			}
			case "MULTIPLE" -> {
				assertThat(question.path("choices").get(0).path("content").asText()).isEqualTo("공개 보기");
				JsonNode correct = question.path("choices").get(0).path("isCorrect");
				assertThat(visible ? correct.asBoolean() : correct.isNull() || correct.isMissingNode()).isTrue();
			}
			case "ORDERING" -> {
				assertThat(question.path("options").get(0).path("content").asText()).isEqualTo("공개 보기");
				JsonNode answerOrder = question.path("options").get(0).path("answerOrder");
				assertThat(visible ? answerOrder.asInt() == 2
					: answerOrder.isNull() || answerOrder.isMissingNode()).isTrue();
			}
			default -> throw new AssertionError("지원하지 않는 문제 유형");
		}
	}

	private MockHttpServletRequestBuilder request(Long questionId, DeliveryMode mode) {
		String path = "/api/v1/question-sets/" + fixture.questionSetId() + "/questions";
		MockHttpServletRequestBuilder request = get(questionId == null ? path : path + "/" + questionId)
			.param("userId", otherUserId == null ? "999999999" : otherUserId.toString());
		return mode == null ? request : request.param("mode", mode.name());
	}

	private void prepareFixture(QuestionSetSolveMode solveMode, QuestionSetStatus state, TeamUserRole role) {
		Long requesterId = ((MaitUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
		fixture = transactions.execute(status -> {
			TeamEntity team = teamEntityRepository.save(TeamEntity.ofGroup("정답 공개 테스트", requesterId));
			List<Long> membershipIds = role == null ? List.of() : List.of(teamUserEntityRepository.save(
				TeamUserEntity.createTeamUser(userEntityRepository.findById(requesterId).orElseThrow(), team, role))
				.getId());
			QuestionSetEntity set = questionSetEntityRepository.save(QuestionSetEntity.builder().title("정답 공개 테스트")
				.teamId(team.getId()).solveMode(solveMode).status(state).build());
			MultipleQuestionEntity multiple = questionEntityRepository.save(MultipleQuestionEntity.builder()
				.questionSet(set)
				.lexoRank("a").number(1L).content("공개 문제").explanation("비공개 해설").answerCount(1).build());
			multipleChoiceEntityRepository.save(MultipleChoiceEntity.builder()
				.question(multiple).number(1).content("공개 보기")
				.isCorrect(true).build());
			ShortQuestionEntity shortQuestion = questionEntityRepository.save(ShortQuestionEntity.builder()
				.questionSet(set)
				.lexoRank("b").number(2L).content("공개 문제").explanation("비공개 해설").answerCount(1).build());
			shortAnswerEntityRepository.save(ShortAnswerEntity.builder()
				.shortQuestionId(shortQuestion.getId()).number(1L)
				.answer("비공개 정답").isMain(true).build());
			FillBlankQuestionEntity blank = questionEntityRepository.save(FillBlankQuestionEntity.builder()
				.questionSet(set)
				.lexoRank("c").number(3L).content("공개 문제").explanation("비공개 해설").build());
			fillBlankAnswerEntityRepository.save(FillBlankAnswerEntity.builder()
				.fillBlankQuestionId(blank.getId()).number(1L)
				.answer("비공개 정답").isMain(true).build());
			OrderingQuestionEntity ordering = questionEntityRepository.save(OrderingQuestionEntity.builder()
				.questionSet(set)
				.lexoRank("d").number(4L).content("공개 문제").explanation("비공개 해설").build());
			orderingOptionEntityRepository.save(OrderingOptionEntity.builder()
				.orderingQuestionId(ordering.getId()).originOrder(1)
				.content("공개 보기").answerOrder(2).build());
			return new Fixture(team.getId(), set.getId(),
				List.of(multiple.getId(), shortQuestion.getId(), blank.getId(), ordering.getId()),
				membershipIds, solveMode);
		});
	}

	private record Fixture(Long teamId, Long questionSetId, List<Long> questionIds, List<Long> membershipIds,
		QuestionSetSolveMode solveMode) {
	}
}
