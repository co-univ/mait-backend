package com.coniv.mait.web.question.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryLinkEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryLinkEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionReader;
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
class QuestionSetWriteAuthorizationApiIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private QuestionReader questionReader;

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
	private QuestionSetCategoryEntityRepository categories;

	@Autowired
	private QuestionSetCategoryLinkEntityRepository categoryLinks;

	private Fixture fixture;

	@AfterEach
	void tearDown() {
		if (fixture == null) {
			return;
		}
		transactions.executeWithoutResult(status -> {
			categoryLinks.deleteAll(categoryLinks.findAllByQuestionSetId(fixture.questionSetId()));
			questions.deleteAllByQuestionSetId(fixture.questionSetId());
			questionSets.deleteById(fixture.questionSetId());
			categories.deleteAllById(List.of(fixture.originalCategoryId(), fixture.newCategoryId()));
			memberships.deleteAllById(fixture.membershipIds());
			teams.deleteAllById(fixture.teamIds());
			users.deleteById(fixture.otherUserId());
		});
	}

	@ParameterizedTest
	@MethodSource("managerCases")
	@DisplayName("해당 팀 MAKER·OWNER와 개인 팀 OWNER가 제목 변경·최종 저장을 수행한다")
	void managerCanWrite(String operation, String role, boolean personal) throws Exception {
		// given
		prepareFixture(role, personal);
		State before = readState();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

		// when & then
		mockMvc.perform(writeRequest(operation))
			.andExpectAll(status().isOk(), jsonPath("$.isSuccess").value(true));
		if (operation.equals("COMPLETE")) {
			QuestionSetStatus expectedStatus = QuestionSetStatus.BEFORE;
			if (personal) {
				expectedStatus = QuestionSetStatus.ONGOING;
			}
			assertThat(readState()).isEqualTo(new State("변경한 제목", QuestionSetSolveMode.STUDY, "변경한 난이도",
				expectedStatus, List.of(1L, 2L), List.of(fixture.newCategoryId())));
		} else {
			assertThat(readState()).isEqualTo(new State("변경한 제목", before.solveMode(), "기존 난이도",
				QuestionSetStatus.MAKING, List.of(9L, 8L), List.of(fixture.originalCategoryId())));
		}
	}

	@ParameterizedTest
	@MethodSource("forbiddenCases")
	@DisplayName("PLAYER·비멤버·다른 팀 관리자의 제목 변경·최종 저장은 403이며 DB가 변경되지 않는다")
	void nonManagerCannotWrite(String operation, String role) throws Exception {
		// given
		prepareFixture(role, false);
		State before = readState();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

		// when & then
		mockMvc.perform(writeRequest(operation))
			.andExpectAll(status().isForbidden(), jsonPath("$.code").value("C-008"),
				jsonPath("$.isSuccess").value(false));
		assertThat(readState()).isEqualTo(before);
	}

	@ParameterizedTest
	@ValueSource(strings = {"TITLE", "COMPLETE"})
	@DisplayName("익명 사용자의 제목 변경·최종 저장은 인증 단계에서 거절한다")
	void anonymousCannotWrite(String operation) throws Exception {
		// given
		prepareFixture("OWNER", false);
		State before = readState();

		// when & then
		mockMvc.perform(writeRequest(operation).with(anonymous()))
			.andExpect(status().isUnauthorized());
		assertThat(readState()).isEqualTo(before);
	}

	private void prepareFixture(String role, boolean personal) {
		MaitUser principal = (MaitUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		Long requesterId = principal.id();
		fixture = transactions.execute(status -> {
			UserEntity requester = users.findById(requesterId).orElseThrow();
			UserEntity other = users.save(UserEntity.localLoginUser(
				"write-auth-" + UUID.randomUUID() + "@test.com", "password", "다른 사용자", "other"));
			UserEntity owner = other;
			if (role.equals("OWNER")) {
				owner = requester;
			}
			TeamEntity team;
			if (personal) {
				team = teams.save(TeamEntity.ofPersonal("개인 팀", owner.getId()));
			} else {
				team = teams.save(TeamEntity.ofGroup("대상 팀", owner.getId()));
			}
			List<Long> teamIds = new ArrayList<>(List.of(team.getId()));
			List<Long> membershipIds = new ArrayList<>();
			membershipIds.add(memberships.save(TeamUserEntity.createOwnerUser(owner, team)).getId());
			if (role.equals("MAKER") || role.equals("PLAYER")) {
				membershipIds.add(memberships.save(
					TeamUserEntity.createTeamUser(requester, team, TeamUserRole.valueOf(role))).getId());
			} else if (role.startsWith("OTHER_")) {
				TeamUserRole otherRole = TeamUserRole.valueOf(role.substring("OTHER_".length()));
				UserEntity otherOwner = requester;
				if (otherRole == TeamUserRole.MAKER) {
					otherOwner = other;
				}
				TeamEntity otherTeam = teams.save(TeamEntity.ofGroup("다른 팀", otherOwner.getId()));
				teamIds.add(otherTeam.getId());
				if (otherRole == TeamUserRole.MAKER) {
					membershipIds.add(memberships.save(TeamUserEntity.createOwnerUser(otherOwner, otherTeam)).getId());
				}
				membershipIds.add(memberships.save(
					TeamUserEntity.createTeamUser(requester, otherTeam, otherRole)).getId());
			}
			QuestionSetSolveMode originalMode = QuestionSetSolveMode.LIVE_TIME;
			if (personal) {
				originalMode = QuestionSetSolveMode.STUDY;
			}
			QuestionSetEntity questionSet = questionSets.save(QuestionSetEntity.builder().teamId(team.getId())
				.creatorId(owner.getId()).title("기존 제목").solveMode(originalMode)
				.difficulty("기존 난이도").status(QuestionSetStatus.MAKING).build());
			questions.save(ShortQuestionEntity.builder().questionSet(questionSet).content("첫 문제")
				.number(9L).lexoRank("a").build());
			questions.save(ShortQuestionEntity.builder().questionSet(questionSet).content("둘째 문제")
				.number(8L).lexoRank("b").build());
			QuestionSetCategoryEntity original = categories.save(QuestionSetCategoryEntity.of(team.getId(), "기존"));
			QuestionSetCategoryEntity added = categories.save(QuestionSetCategoryEntity.of(team.getId(), "변경"));
			categoryLinks.save(QuestionSetCategoryLinkEntity.of(questionSet.getId(), original.getId()));
			return new Fixture(questionSet.getId(), owner.getId(), other.getId(), original.getId(), added.getId(),
				teamIds, membershipIds);
		});
	}

	private MockHttpServletRequestBuilder writeRequest(String operation) throws Exception {
		Map<String, Object> body = Map.of("title", "변경한 제목", "solveMode", "STUDY",
			"difficulty", "변경한 난이도", "categoryIds", List.of(fixture.newCategoryId()), "userId", fixture.ownerId());
		MockHttpServletRequestBuilder request = patch("/api/v1/question-sets/{id}", fixture.questionSetId());
		if (operation.equals("COMPLETE")) {
			request = put("/api/v1/question-sets/{id}", fixture.questionSetId());
		}
		return request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
	}

	private State readState() {
		return transactions.execute(status -> {
			QuestionSetEntity questionSet = questionSets.findById(fixture.questionSetId()).orElseThrow();
			List<Long> questionNumbers = questionReader.getOrderedQuestions(questionSet.getId())
				.stream().map(QuestionEntity::getNumber).toList();
			List<Long> categoryIds = categoryLinks.findAllByQuestionSetId(questionSet.getId()).stream()
				.map(QuestionSetCategoryLinkEntity::getCategoryId).sorted().toList();
			return new State(questionSet.getTitle(), questionSet.getSolveMode(), questionSet.getDifficulty(),
				questionSet.getStatus(), questionNumbers, categoryIds);
		});
	}

	private static Stream<Arguments> managerCases() {
		return Stream.of("TITLE", "COMPLETE").flatMap(operation -> Stream.of(
			Arguments.of(operation, "MAKER", false), Arguments.of(operation, "OWNER", false),
			Arguments.of(operation, "OWNER", true)));
	}

	private static Stream<Arguments> forbiddenCases() {
		return Stream.of("TITLE", "COMPLETE").flatMap(operation ->
			Stream.of("PLAYER", "NON_MEMBER", "OTHER_MAKER", "OTHER_OWNER")
				.map(role -> Arguments.of(operation, role)));
	}

	private record Fixture(Long questionSetId, Long ownerId, Long otherUserId, Long originalCategoryId,
		Long newCategoryId, List<Long> teamIds, List<Long> membershipIds) {
	}

	private record State(String title, QuestionSetSolveMode solveMode, String difficulty, QuestionSetStatus status,
		List<Long> questionNumbers, List<Long> categoryIds) {
	}
}
