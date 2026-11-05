package com.coniv.mait.web.solve.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
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

@WithCustomUser
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class QuestionSubmitRecordsApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private TransactionTemplate transactions;

	@Autowired
	private UserEntityRepository users;

	@Autowired
	private TeamEntityRepository teams;

	@Autowired
	private TeamUserEntityRepository memberships;

	@Autowired
	private QuestionSetEntityRepository questionSets;

	@Autowired
	private QuestionEntityRepository questions;

	@Autowired
	private AnswerSubmitRecordEntityRepository submissions;

	private Fixture fixture;

	@AfterEach
	void tearDown() {
		if (fixture == null) {
			return;
		}
		transactions.executeWithoutResult(status -> {
			submissions.deleteAllByQuestionIdIn(List.of(fixture.questionId()));
			questions.deleteById(fixture.questionId());
			questionSets.deleteById(fixture.questionSetId());
			memberships.deleteAllById(fixture.membershipIds());
			teams.deleteById(fixture.teamId());
			users.deleteById(fixture.submitterId());
		});
	}

	@ParameterizedTest
	@CsvSource({"LIVE_TIME, MAKER", "LIVE_TIME, OWNER", "STUDY, MAKER", "STUDY, OWNER"})
	@DisplayName("테스트 트랜잭션 없이 관리자가 실시간·학습모드 미응답 기록을 조회한다")
	void getSubmitRecords_Manager_ReturnsRecords(QuestionSetSolveMode solveMode, TeamUserRole role) throws Exception {
		// given
		prepareFixture(solveMode, role);
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

		// when & then
		mockMvc.perform(get(path(), fixture.questionSetId(), fixture.questionId()))
			.andExpectAll(
				status().isOk(),
				jsonPath("$.isSuccess").value(true),
				jsonPath("$.data.totalCounts").value(1),
				jsonPath("$.data.submitRecords[0].id").value(fixture.recordId()),
				jsonPath("$.data.submitRecords[0].userId").value(fixture.submitterId()),
				jsonPath("$.data.submitRecords[0].questionId").value(fixture.questionId()),
				jsonPath("$.data.submitRecords[0].isCorrect").value(false),
				jsonPath("$.data.submitRecords[0].submitOrder").isEmpty(),
				jsonPath("$.data.submitRecords[0].submittedAnswer").isEmpty());
		assertThat(submissions.findAllByQuestionId(fixture.questionId())).hasSize(1);
	}

	@ParameterizedTest
	@CsvSource({"LIVE_TIME, PLAYER", "STUDY, PLAYER", "LIVE_TIME, NON_MEMBER", "STUDY, NON_MEMBER"})
	@DisplayName("테스트 트랜잭션 없이 PLAYER와 비멤버의 제출 기록 조회를 403으로 차단한다")
	void getSubmitRecords_NotManager_ReturnsForbidden(QuestionSetSolveMode solveMode, String role) throws Exception {
		// given
		prepareFixture(solveMode, role.equals("NON_MEMBER") ? null : TeamUserRole.valueOf(role));
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

		// when & then
		mockMvc.perform(get(path(), fixture.questionSetId(), fixture.questionId()))
			.andExpectAll(
				status().isForbidden(),
				jsonPath("$.code").value("C-008"),
				jsonPath("$.isSuccess").value(false),
				jsonPath("$.data").doesNotExist());
	}

	@ParameterizedTest
	@CsvSource({"false, 404, C-002", "true, 400, C-004"})
	@DisplayName("존재하지 않는 문제와 문제셋 소속 불일치의 오류 응답을 유지한다")
	void getSubmitRecords_InvalidQuestionScope_ReturnsClientError(boolean mismatch, int expectedStatus,
		String expectedCode) throws Exception {
		// given
		prepareFixture(QuestionSetSolveMode.LIVE_TIME, TeamUserRole.MAKER);
		Long questionSetId = mismatch ? Long.MAX_VALUE : fixture.questionSetId();
		Long questionId = mismatch ? fixture.questionId() : Long.MAX_VALUE;

		// when & then
		mockMvc.perform(get(path(), questionSetId, questionId))
			.andExpectAll(
				status().is(expectedStatus),
				jsonPath("$.code").value(expectedCode),
				jsonPath("$.isSuccess").value(false));
	}

	private void prepareFixture(QuestionSetSolveMode solveMode, TeamUserRole role) {
		Long requesterId = ((MaitUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
		fixture = transactions.execute(status -> {
			UserEntity requester = users.findById(requesterId).orElseThrow();
			UserEntity submitter = users.save(UserEntity.localLoginUser(
				"submitter-" + UUID.randomUUID() + "@test.com", "password", "제출자", "submitter"));
			Long creatorId = role == TeamUserRole.OWNER ? requesterId : submitter.getId();
			TeamEntity team = teams.save(TeamEntity.ofGroup("제출 기록 테스트", creatorId));
			TeamUserEntity submitterMember = memberships.save(TeamUserEntity.createTeamUser(submitter, team,
				role == TeamUserRole.OWNER ? TeamUserRole.PLAYER : TeamUserRole.OWNER));
			List<Long> membershipIds;
			if (role == null) {
				membershipIds = List.of(submitterMember.getId());
			} else {
				TeamUserEntity requesterMember = memberships.save(TeamUserEntity.createTeamUser(requester, team, role));
				membershipIds = List.of(submitterMember.getId(), requesterMember.getId());
			}
			QuestionSetEntity questionSet = questionSets.save(QuestionSetEntity.builder()
				.title("제출 기록 테스트").teamId(team.getId()).solveMode(solveMode).status(QuestionSetStatus.AFTER)
				.build());
			MultipleQuestionEntity question = questions.save(MultipleQuestionEntity.builder()
				.questionSet(questionSet).number(1L).lexoRank("m").content("테스트 문제").answerCount(2).build());
			AnswerSubmitRecordEntity record = submissions.save(AnswerSubmitRecordEntity.builder()
				.questionId(question.getId()).userId(submitter.getId()).isCorrect(false).build());
			return new Fixture(team.getId(), questionSet.getId(), question.getId(), submitter.getId(), membershipIds,
				record.getId());
		});
	}

	private String path() {
		return "/api/v1/question-sets/{questionSetId}/questions/{questionId}/submit-records";
	}

	private record Fixture(Long teamId, Long questionSetId, Long questionId, Long submitterId,
		List<Long> membershipIds, Long recordId) {
	}
}
