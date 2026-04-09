package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetCreationType;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.QuestionSetVisibility;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;
import com.coniv.mait.web.question.dto.UpdateQuestionSetApiRequest;
import com.coniv.mait.web.question.dto.UpdateQuestionSetFieldApiRequest;

@WithCustomUser
public class QuestionSetApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Autowired
	private TeamEntityRepository teamEntityRepository;

	@Autowired
	private QuestionEntityRepository questionEntityRepository;

	@Autowired
	private MultipleChoiceEntityRepository multipleChoiceEntityRepository;

	@Autowired
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	@Autowired
	private UserEntityRepository userEntityRepository;

	@BeforeEach
	void setUp() {
		solvingSessionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		multipleChoiceEntityRepository.deleteAll();
	}

	// @Test
	// @DisplayName("문제 셋 생성 API 성공 테스트")
	// void createQuestionSetApiSuccess() throws Exception {
	// 	// given
	// 	String subject = "Sample Subject";
	// 	QuestionSetCreationType creationType = QuestionSetCreationType.MANUAL;
	//
	// 	CreateQuestionSetApiRequest request = new CreateQuestionSetApiRequest(subject, creationType);
	//
	// 	// when
	// 	mockMvc.perform(post("/api/v1/question-sets").contentType(MediaType.APPLICATION_JSON)
	// 			.content(objectMapper.writeValueAsString(request)))
	// 		.andExpect(status().isCreated())
	// 		.andExpect(jsonPath("$.data.subject").value(subject));
	//
	// 	// then
	// 	QuestionSetEntity questionSetEntity = questionSetEntityRepository.findAll().get(0);
	//
	// 	assertThat(questionSetEntityRepository.count()).isEqualTo(1);
	// 	assertThat(questionSetEntity.getSubject()).isEqualTo(subject);
	// 	assertThat(questionSetEntity.getCreationType()).isEqualTo(creationType);
	// }

	@Test
	@DisplayName("문제 셋 목록 조회 API 성공 테스트 - MAKING 모드 (List 구조)")
	void getQuestionSetsApiSuccess_MakingMode() throws Exception {
		// given
		TeamEntity team = teamEntityRepository.save(TeamEntity.builder().name("코니브").creatorId(1L).build());
		String subject1 = "Subject 1";
		String subject2 = "Subject 2";
		final DeliveryMode deliveryMode = DeliveryMode.MAKING;

		QuestionSetEntity questionSet1 = QuestionSetEntity.builder()
			.subject(subject1)
			.teamId(team.getId())
			.build();
		QuestionSetEntity questionSet2 = QuestionSetEntity.builder()
			.subject(subject2)
			.teamId(team.getId())
			.build();

		questionSetEntityRepository.save(questionSet1);
		questionSetEntityRepository.save(questionSet2);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets")
				.param("teamId", String.valueOf(team.getId()))
				.param("mode", deliveryMode.name())
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.mode").value("MAKING"))
			.andExpect(jsonPath("$.data.content.questionSets").isArray())
			.andExpect(jsonPath("$.data.content.questionSets.length()").value(2))
			.andExpect(jsonPath("$.data.content.questionSets[0].subject").value(subject2)) // 최신 것이 먼저
			.andExpect(jsonPath("$.data.content.questionSets[1].subject").value(subject1));

		// then
		assertThat(questionSetEntityRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("문제 셋 목록 조회 API 성공 테스트 - LIVE_TIME 모드 (Map 구조)")
	void getQuestionSetsApiSuccess_LiveTimeMode() throws Exception {
		// given
		TeamEntity team = teamEntityRepository.save(TeamEntity.builder().name("코니브").creatorId(1L).build());
		final DeliveryMode deliveryMode = DeliveryMode.LIVE_TIME;

		// BEFORE 상태 문제 셋
		QuestionSetEntity beforeSet = QuestionSetEntity.builder()
			.subject("시작 전 문제")
			.teamId(team.getId())
			.status(QuestionSetStatus.BEFORE)
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.build();

		// ONGOING 상태 문제 셋
		QuestionSetEntity ongoingSet = QuestionSetEntity.builder()
			.subject("진행 중 문제")
			.teamId(team.getId())
			.status(QuestionSetStatus.ONGOING)
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.build();

		// AFTER 상태 문제 셋
		QuestionSetEntity afterSet = QuestionSetEntity.builder()
			.subject("종료된 문제")
			.teamId(team.getId())
			.status(QuestionSetStatus.AFTER)
			.solveMode(QuestionSetSolveMode.LIVE_TIME)
			.build();

		questionSetEntityRepository.save(beforeSet);
		questionSetEntityRepository.save(ongoingSet);
		questionSetEntityRepository.save(afterSet);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets")
				.param("teamId", String.valueOf(team.getId()))
				.param("mode", deliveryMode.name())
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.mode").value("LIVE_TIME"))
			.andExpect(jsonPath("$.data.content.questionSets").isMap())
			.andExpect(jsonPath("$.data.content.questionSets.BEFORE").isArray())
			.andExpect(jsonPath("$.data.content.questionSets.BEFORE.length()").value(1))
			.andExpect(jsonPath("$.data.content.questionSets.BEFORE[0].subject").value("시작 전 문제"))
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING").isArray())
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING.length()").value(1))
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING[0].subject").value("진행 중 문제"))
			.andExpect(jsonPath("$.data.content.questionSets.AFTER").isArray())
			.andExpect(jsonPath("$.data.content.questionSets.AFTER.length()").value(1))
			.andExpect(jsonPath("$.data.content.questionSets.AFTER[0].subject").value("종료된 문제"));

		// then
		assertThat(questionSetEntityRepository.count()).isEqualTo(3);
	}

	@Test
	@DisplayName("문제 셋 목록 조회 API 성공 테스트 - STUDY 모드에서 사용자별 풀이 상태 포함")
	void getQuestionSetsApiSuccess_StudyModeWithUserSolveStatus() throws Exception {
		// given
		UserEntity currentUser = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(TeamEntity.builder().name("코니브").creatorId(1L).build());
		final DeliveryMode deliveryMode = DeliveryMode.STUDY;

		QuestionSetEntity notStartedSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("아직 안 푼 문제")
				.teamId(team.getId())
				.solveMode(QuestionSetSolveMode.STUDY)
				.status(QuestionSetStatus.ONGOING)
				.build());

		QuestionSetEntity progressingSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("풀고 있는 문제")
				.teamId(team.getId())
				.solveMode(QuestionSetSolveMode.STUDY)
				.status(QuestionSetStatus.ONGOING)
				.build());

		QuestionSetEntity completedSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("채점 완료 문제")
				.teamId(team.getId())
				.solveMode(QuestionSetSolveMode.STUDY)
				.status(QuestionSetStatus.ONGOING)
				.build());

		solvingSessionEntityRepository.save(SolvingSessionEntity.studySession(currentUser, progressingSet));

		SolvingSessionEntity completedSession = SolvingSessionEntity.studySession(currentUser, completedSet);
		completedSession.submit(3, 2);
		solvingSessionEntityRepository.save(completedSession);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets")
				.param("teamId", String.valueOf(team.getId()))
				.param("mode", deliveryMode.name())
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.mode").value("STUDY"))
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING").isArray())
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING.length()").value(3))
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING[?(@.subject=='채점 완료 문제')].userSolveStatus")
				.value(contains("COMPLETED")))
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING[?(@.subject=='풀고 있는 문제')].userSolveStatus")
				.value(contains("IN_PROGRESS")))
			.andExpect(jsonPath("$.data.content.questionSets.ONGOING[?(@.subject=='아직 안 푼 문제')].userSolveStatus")
				.value(contains("NOT_STARTED")));
	}

	@Test
	@DisplayName("문제 셋 단건 조회 API 성공 테스트")
	void getQuestionSetApiSuccess() throws Exception {
		// given
		String subject = "Sample Subject";
		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder().subject(subject).build());

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/{questionSetId}", questionSet.getId())
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(questionSet.getId()))
			.andExpect(jsonPath("$.data.subject").value(subject));
	}

	@Test
	@DisplayName("문제 셋 최종 저장 API 성공 테스트")
	void updateQuestionSetsApiSuccess() throws Exception {
		// given
		UpdateQuestionSetApiRequest request = new UpdateQuestionSetApiRequest(
			"Updated Title",
			"Updated Subject",
			QuestionSetSolveMode.LIVE_TIME,
			"중급",
			QuestionSetVisibility.GROUP);

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("Initial Subject")
				.creationType(QuestionSetCreationType.MANUAL)
				.build());

		// when & then
		mockMvc.perform(put("/api/v1/question-sets/{questionSetId}", questionSet.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(questionSet.getId()))
			.andExpect(jsonPath("$.data.title").value("Updated Title"))
			.andExpect(jsonPath("$.data.subject").value("Updated Subject"))
			.andExpect(jsonPath("$.data.deliveryMode").value(DeliveryMode.LIVE_TIME.name()));
	}

	@Test
	@DisplayName("문제 셋 제목 변경 API 성공 테스트")
	void updateQuestionSetTitleApiSuccess() throws Exception {
		// given
		final String title = "Updated Title";
		UpdateQuestionSetFieldApiRequest request = new UpdateQuestionSetFieldApiRequest(title);
		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("Initial Subject")
				.creationType(QuestionSetCreationType.MANUAL)
				.build());

		// when & then
		mockMvc.perform(patch("/api/v1/question-sets/{questionSetId}", questionSet.getId())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpectAll(status().isOk(),
				jsonPath("$.isSuccess").value(true));

		QuestionSetEntity findSet = questionSetEntityRepository.findById(questionSet.getId()).get();
		assertThat(findSet.getTitle()).isEqualTo(title);
	}

	@Test
	@DisplayName("문제 셋 검증 API 통합 테스트 - 모든 문제가 유효한 경우")
	void validateQuestionSet_AllValid_IntegrationTest() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 주제", QuestionSetCreationType.MANUAL);
		questionSetEntityRepository.save(questionSet);

		MultipleQuestionEntity question1 = MultipleQuestionEntity.builder()
			.questionSet(questionSet)
			.content("객관식 문제 1")
			.number(1L)
			.lexoRank("a")
			.build();
		questionEntityRepository.save(question1);

		MultipleChoiceEntity choice1 = MultipleChoiceEntity.builder()
			.question(question1)
			.content("선택지 1")
			.isCorrect(true)
			.number(1)
			.build();

		MultipleChoiceEntity choice2 = MultipleChoiceEntity.builder()
			.question(question1)
			.content("선택지 2")
			.isCorrect(false)
			.number(2)
			.build();

		multipleChoiceEntityRepository.saveAll(java.util.List.of(choice1, choice2));

		MultipleQuestionEntity question2 = MultipleQuestionEntity.builder()
			.questionSet(questionSet)
			.content("객관식 문제 2")
			.number(2L)
			.lexoRank("b")
			.build();
		questionEntityRepository.save(question2);

		MultipleChoiceEntity choice3 = MultipleChoiceEntity.builder()
			.question(question2)
			.content("선택지 A")
			.isCorrect(false)
			.number(1)
			.build();

		MultipleChoiceEntity choice4 = MultipleChoiceEntity.builder()
			.question(question2)
			.content("선택지 B")
			.isCorrect(true)
			.number(2)
			.build();

		MultipleChoiceEntity choice5 = MultipleChoiceEntity.builder()
			.question(question2)
			.content("선택지 C")
			.isCorrect(false)
			.number(3)
			.build();

		multipleChoiceEntityRepository.saveAll(java.util.List.of(choice3, choice4, choice5));

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/validate")
				.param("questionSetId", String.valueOf(questionSet.getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(0));
	}

	@Test
	@DisplayName("문제 셋 검증 API 통합 테스트 - 일부 문제가 유효하지 않은 경우")
	void validateQuestionSet_SomeInvalid_IntegrationTest() throws Exception {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.of("테스트 주제", QuestionSetCreationType.MANUAL);
		questionSetEntityRepository.save(questionSet);

		// 유효한 문제
		MultipleQuestionEntity validQuestion = MultipleQuestionEntity.builder()
			.questionSet(questionSet)
			.content("유효한 객관식 문제")
			.number(1L)
			.lexoRank("a")
			.build();
		questionEntityRepository.save(validQuestion);

		MultipleChoiceEntity validChoice1 = MultipleChoiceEntity.builder()
			.question(validQuestion)
			.content("선택지 1")
			.isCorrect(true)
			.number(1)
			.build();

		MultipleChoiceEntity validChoice2 = MultipleChoiceEntity.builder()
			.question(validQuestion)
			.content("선택지 2")
			.isCorrect(false)
			.number(2)
			.build();

		multipleChoiceEntityRepository.saveAll(java.util.List.of(validChoice1, validChoice2));

		// 내용이 없는 문제 (유효하지 않음)
		MultipleQuestionEntity invalidQuestion1 = MultipleQuestionEntity.builder()
			.questionSet(questionSet)
			.content(null)
			.number(2L)
			.lexoRank("b")
			.build();
		questionEntityRepository.save(invalidQuestion1);

		MultipleChoiceEntity invalidChoice1 = MultipleChoiceEntity.builder()
			.question(invalidQuestion1)
			.content("선택지 A")
			.isCorrect(true)
			.number(1)
			.build();

		MultipleChoiceEntity invalidChoice2 = MultipleChoiceEntity.builder()
			.question(invalidQuestion1)
			.content("선택지 B")
			.isCorrect(false)
			.number(2)
			.build();

		multipleChoiceEntityRepository.saveAll(java.util.List.of(invalidChoice1, invalidChoice2));

		// 선택지가 1개만 있는 문제 (유효하지 않음)
		MultipleQuestionEntity invalidQuestion2 = MultipleQuestionEntity.builder()
			.questionSet(questionSet)
			.content("선택지가 부족한 문제")
			.number(3L)
			.lexoRank("c")
			.build();
		questionEntityRepository.save(invalidQuestion2);

		MultipleChoiceEntity invalidChoice3 = MultipleChoiceEntity.builder()
			.question(invalidQuestion2)
			.content("유일한 선택지")
			.isCorrect(true)
			.number(1)
			.build();

		multipleChoiceEntityRepository.save(invalidChoice3);

		// when & then
		mockMvc.perform(get("/api/v1/question-sets/validate")
				.param("questionSetId", String.valueOf(questionSet.getId())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].questionId").value(invalidQuestion1.getId()))
			.andExpect(jsonPath("$.data[0].isValid").value(false))
			.andExpect(jsonPath("$.data[0].number").value(2L))
			.andExpect(jsonPath("$.data[1].questionId").value(invalidQuestion2.getId()))
			.andExpect(jsonPath("$.data[1].isValid").value(false))
			.andExpect(jsonPath("$.data[1].number").value(3L));
	}
}
