package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.coniv.mait.domain.question.entity.FillBlankAnswerEntity;
import com.coniv.mait.domain.question.entity.FillBlankQuestionEntity;
import com.coniv.mait.domain.question.entity.MultipleChoiceEntity;
import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.OrderingOptionEntity;
import com.coniv.mait.domain.question.entity.OrderingQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.entity.ShortAnswerEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.FillBlankAnswerEntityRepository;
import com.coniv.mait.domain.question.repository.MultipleChoiceEntityRepository;
import com.coniv.mait.domain.question.repository.OrderingOptionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.question.repository.ShortAnswerEntityRepository;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.solve.repository.StudyAnswerDraftEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.login.WithCustomUser;
import com.coniv.mait.web.integration.BaseIntegrationTest;

@WithCustomUser
public class QuestionSetDeleteApiIntegrationTest extends BaseIntegrationTest {

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
	private FillBlankAnswerEntityRepository fillBlankAnswerEntityRepository;

	@Autowired
	private OrderingOptionEntityRepository orderingOptionEntityRepository;

	@Autowired
	private QuestionSetParticipantRepository questionSetParticipantRepository;

	@Autowired
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	@Autowired
	private StudyAnswerDraftEntityRepository studyAnswerDraftEntityRepository;

	@Autowired
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Autowired
	private QuestionScorerEntityRepository questionScorerEntityRepository;

	@BeforeEach
	void clear() {
		studyAnswerDraftEntityRepository.deleteAll();
		answerSubmitRecordEntityRepository.deleteAll();
		questionScorerEntityRepository.deleteAll();
		solvingSessionEntityRepository.deleteAll();
		questionSetParticipantRepository.deleteAll();
		multipleChoiceEntityRepository.deleteAll();
		shortAnswerEntityRepository.deleteAll();
		fillBlankAnswerEntityRepository.deleteAll();
		orderingOptionEntityRepository.deleteAll();
		questionEntityRepository.deleteAll();
		questionSetEntityRepository.deleteAll();
	}

	@Test
	@DisplayName("BEFORE 상태 문제셋 삭제 - 혼합 타입 문제와 모든 하위 엔티티가 삭제된다")
	void deleteQuestionSet_beforeStatus_deletesAllQuestionsAndSubEntities() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("팀A").creatorId(user.getId()).build());
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(user, team, TeamUserRole.MAKER));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("BEFORE 문제셋")
				.teamId(team.getId())
				.status(QuestionSetStatus.BEFORE)
				.build());

		MultipleQuestionEntity multipleQ = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet).lexoRank("a").answerCount(1).build());
		multipleChoiceEntityRepository.save(
			MultipleChoiceEntity.builder()
				.question(multipleQ).number(1).content("선택지1").isCorrect(true).build());

		ShortQuestionEntity shortQ = questionEntityRepository.save(
			ShortQuestionEntity.builder()
				.questionSet(questionSet).lexoRank("b").answerCount(1).build());
		shortAnswerEntityRepository.save(
			ShortAnswerEntity.builder()
				.shortQuestionId(shortQ.getId()).answer("정답").isMain(true).number(1L).build());

		FillBlankQuestionEntity fillBlankQ = questionEntityRepository.save(
			FillBlankQuestionEntity.builder()
				.questionSet(questionSet).lexoRank("c").build());
		fillBlankAnswerEntityRepository.save(
			FillBlankAnswerEntity.builder()
				.fillBlankQuestionId(fillBlankQ.getId()).answer("빈칸정답").isMain(true).number(1L).build());

		OrderingQuestionEntity orderingQ = questionEntityRepository.save(
			OrderingQuestionEntity.builder()
				.questionSet(questionSet).lexoRank("d").build());
		orderingOptionEntityRepository.save(
			OrderingOptionEntity.builder()
				.orderingQuestionId(orderingQ.getId()).originOrder(1).answerOrder(1).content("보기1").build());

		// when
		mockMvc.perform(delete("/api/v1/question-sets/{questionSetId}", questionSet.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true));

		// then
		assertThat(questionSetEntityRepository.existsById(questionSet.getId())).isFalse();
		assertThat(questionEntityRepository.count()).isZero();
		assertThat(multipleChoiceEntityRepository.count()).isZero();
		assertThat(shortAnswerEntityRepository.count()).isZero();
		assertThat(fillBlankAnswerEntityRepository.count()).isZero();
		assertThat(orderingOptionEntityRepository.count()).isZero();
	}

	@Test
	@DisplayName("LIVE_TIME + AFTER 상태 문제셋 삭제 - 참가자, 채점자, 제출기록까지 삭제된다")
	void deleteQuestionSet_liveTimeAfter_deletesParticipantsScorersAndSubmitRecords() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("팀B").creatorId(user.getId()).build());
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(user, team, TeamUserRole.MAKER));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("LIVE_TIME AFTER 문제셋")
				.teamId(team.getId())
				.solveMode(QuestionSetSolveMode.LIVE_TIME)
				.status(QuestionSetStatus.AFTER)
				.build());

		MultipleQuestionEntity question = questionEntityRepository.save(
			MultipleQuestionEntity.builder()
				.questionSet(questionSet).lexoRank("a").answerCount(1).build());
		multipleChoiceEntityRepository.save(
			MultipleChoiceEntity.builder()
				.question(question).number(1).content("선택지1").isCorrect(true).build());

		questionSetParticipantRepository.save(
			QuestionSetParticipantEntity.createActiveParticipant(questionSet, user));

		answerSubmitRecordEntityRepository.save(
			AnswerSubmitRecordEntity.builder()
				.userId(user.getId()).questionId(question.getId())
				.submitOrder(1L).isCorrect(true).submittedAnswer("{}").build());

		questionScorerEntityRepository.save(
			QuestionScorerEntity.builder()
				.questionId(question.getId()).userId(user.getId()).submitOrder(1L).build());

		// when
		mockMvc.perform(delete("/api/v1/question-sets/{questionSetId}", questionSet.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true));

		// then
		assertThat(questionSetEntityRepository.existsById(questionSet.getId())).isFalse();
		assertThat(questionEntityRepository.count()).isZero();
		assertThat(multipleChoiceEntityRepository.count()).isZero();
		assertThat(questionSetParticipantRepository.count()).isZero();
		assertThat(answerSubmitRecordEntityRepository.count()).isZero();
		assertThat(questionScorerEntityRepository.count()).isZero();
	}

	@Test
	@DisplayName("STUDY + ONGOING 상태 문제셋 삭제 - 풀이 세션과 임시 답안까지 삭제된다")
	void deleteQuestionSet_studyOngoing_deletesSessionsAndDrafts() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("팀C").creatorId(user.getId()).build());
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(user, team, TeamUserRole.MAKER));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("STUDY ONGOING 문제셋")
				.teamId(team.getId())
				.solveMode(QuestionSetSolveMode.STUDY)
				.status(QuestionSetStatus.ONGOING)
				.build());

		ShortQuestionEntity question = questionEntityRepository.save(
			ShortQuestionEntity.builder()
				.questionSet(questionSet).lexoRank("a").answerCount(1).build());
		shortAnswerEntityRepository.save(
			ShortAnswerEntity.builder()
				.shortQuestionId(question.getId()).answer("정답").isMain(true).number(1L).build());

		answerSubmitRecordEntityRepository.save(
			AnswerSubmitRecordEntity.builder()
				.userId(user.getId()).questionId(question.getId())
				.submitOrder(1L).isCorrect(true).submittedAnswer("{}").build());

		SolvingSessionEntity session = solvingSessionEntityRepository.save(
			SolvingSessionEntity.studySession(user, questionSet));
		studyAnswerDraftEntityRepository.save(StudyAnswerDraftEntity.of(session, question.getId()));

		// when
		mockMvc.perform(delete("/api/v1/question-sets/{questionSetId}", questionSet.getId()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true));

		// then
		assertThat(questionSetEntityRepository.existsById(questionSet.getId())).isFalse();
		assertThat(questionEntityRepository.count()).isZero();
		assertThat(shortAnswerEntityRepository.count()).isZero();
		assertThat(answerSubmitRecordEntityRepository.count()).isZero();
		assertThat(solvingSessionEntityRepository.count()).isZero();
		assertThat(studyAnswerDraftEntityRepository.count()).isZero();
	}

	@Test
	@DisplayName("LIVE_TIME + ONGOING 상태 문제셋 삭제 시도 - 409 응답을 반환한다")
	void deleteQuestionSet_liveTimeOngoing_returns409() throws Exception {
		// given
		UserEntity user = userEntityRepository.findByEmail("user@example.com").orElseThrow();
		TeamEntity team = teamEntityRepository.save(
			TeamEntity.builder().name("팀D").creatorId(user.getId()).build());
		teamUserEntityRepository.save(TeamUserEntity.createTeamUser(user, team, TeamUserRole.MAKER));

		QuestionSetEntity questionSet = questionSetEntityRepository.save(
			QuestionSetEntity.builder()
				.subject("진행중 문제셋")
				.teamId(team.getId())
				.solveMode(QuestionSetSolveMode.LIVE_TIME)
				.status(QuestionSetStatus.ONGOING)
				.build());

		// when & then
		mockMvc.perform(delete("/api/v1/question-sets/{questionSetId}", questionSet.getId()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.isSuccess").value(false))
			.andExpect(jsonPath("$.code").value("3001"));
	}
}
