package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.coniv.mait.domain.question.entity.QuestionSetCategoryEntity;
import com.coniv.mait.domain.question.entity.QuestionSetCategoryLinkEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.entity.ShortQuestionEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.exception.QuestionSetStatusException;
import com.coniv.mait.domain.question.exception.code.QuestionSetStatusExceptionCode;
import com.coniv.mait.domain.question.repository.AiRequestStatusManager;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetCategoryLinkEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.service.component.QuestionChecker;
import com.coniv.mait.domain.question.service.component.QuestionSetReader;
import com.coniv.mait.domain.question.service.component.QuestionWebSocketSender;
import com.coniv.mait.domain.question.service.dto.QuestionSetCategoryDto;
import com.coniv.mait.domain.question.service.dto.QuestionSetDto;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.domain.team.entity.TeamUserEntity;
import com.coniv.mait.domain.team.enums.TeamUserRole;
import com.coniv.mait.domain.team.repository.TeamEntityRepository;
import com.coniv.mait.domain.team.repository.TeamUserEntityRepository;
import com.coniv.mait.domain.team.service.component.TeamReader;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.event.MaitEventPublisher;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Import({QuestionSetService.class, QuestionSetReader.class, QuestionSetCategoryService.class,
	TeamRoleValidator.class, TeamReader.class})
class QuestionSetModeIntegrationTest {

	@Autowired
	private QuestionSetService service;
	@Autowired
	private QuestionSetEntityRepository questionSets;
	@Autowired
	private QuestionEntityRepository questions;
	@Autowired
	private QuestionSetCategoryEntityRepository categories;
	@Autowired
	private QuestionSetCategoryLinkEntityRepository categoryLinks;
	@Autowired
	private SolvingSessionEntityRepository sessions;
	@Autowired
	private TeamEntityRepository teams;
	@Autowired
	private TeamUserEntityRepository members;
	@Autowired
	private UserEntityRepository users;
	@Autowired
	private EntityManager entityManager;

	@MockitoBean
	private QuestionService questionService;
	@MockitoBean
	private MaitEventPublisher eventPublisher;
	@MockitoBean
	private QuestionChecker questionChecker;
	@MockitoBean
	private QuestionSetMaterialService materialService;
	@MockitoBean
	private AiRequestStatusManager aiRequestStatusManager;
	@MockitoBean
	private QuestionWebSocketSender webSocketSender;
	@MockitoBean
	private QuestionSetParticipantService participantService;
	@MockitoBean
	private SimpMessagingTemplate messagingTemplate;

	private UserEntity user;
	private TeamEntity team;
	private MaitUser principal;

	@BeforeEach
	void setUp() {
		user = users.save(UserEntity.localLoginUser(UUID.randomUUID() + "@test.com", "password", "maker", null));
		team = teams.save(TeamEntity.ofGroup("모드 변경 팀", user.getId()));
		members.save(TeamUserEntity.createTeamUser(user, team, TeamUserRole.MAKER));
		principal = MaitUser.builder().id(user.getId()).build();
	}

	@ParameterizedTest
	@EnumSource(QuestionSetSolveMode.class)
	@DisplayName("진행 예정 문제 셋은 양방향 모드 변경과 재요청 후에도 다른 데이터를 보존한다")
	void changeSolveMode_preservesData(QuestionSetSolveMode originalMode) {
		QuestionSetEntity questionSet = saveQuestionSet(QuestionSetStatus.BEFORE, originalMode);
		var question = questions.save(ShortQuestionEntity.builder().questionSet(questionSet)
			.content("문제").lexoRank("a").number(7L).build());
		var category = categories.save(QuestionSetCategoryEntity.of(team.getId(), "보존할 카테고리"));
		categoryLinks.save(QuestionSetCategoryLinkEntity.of(questionSet.getId(), category.getId()));
		QuestionSetSolveMode targetMode = opposite(originalMode);
		entityManager.flush();
		entityManager.clear();

		QuestionSetDto response = service.changeSolveMode(questionSet.getId(), targetMode, principal);
		service.changeSolveMode(questionSet.getId(), targetMode, principal);
		entityManager.flush();
		entityManager.clear();

		QuestionSetEntity saved = questionSets.findById(questionSet.getId()).orElseThrow();
		assertThat(saved.getSolveMode()).isEqualTo(targetMode);
		assertThat(saved.getStatus()).isEqualTo(QuestionSetStatus.BEFORE);
		assertThat(saved.getTitle()).isEqualTo("유지할 제목");
		assertThat(saved.getDifficulty()).isEqualTo("유지할 난이도");
		assertThat(saved.getInstruction()).isEqualTo("유지할 설명");
		assertThat(saved.getStartTime()).isNull();
		assertThat(saved.getEndTime()).isNull();
		assertThat(questions.findById(question.getId()).orElseThrow().getNumber()).isEqualTo(7L);
		assertThat(categoryLinks.findAllByQuestionSetId(saved.getId()))
			.extracting(QuestionSetCategoryLinkEntity::getCategoryId).containsExactly(category.getId());
		assertThat(response.getCategories()).extracting(QuestionSetCategoryDto::getId)
			.containsExactly(category.getId());
		assertThat(response.getQuestionCount()).isEqualTo(1L);
	}

	@ParameterizedTest
	@EnumSource(value = QuestionSetStatus.class, names = "BEFORE", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("진행 예정 이외의 상태에서는 기존 문제 셋의 풀이 방식 변경을 차단한다")
	void changeSolveMode_rejectsInvalidStatus(QuestionSetStatus status) {
		QuestionSetEntity questionSet = saveQuestionSet(status, QuestionSetSolveMode.LIVE_TIME);
		assertThatThrownBy(() -> service.changeSolveMode(questionSet.getId(), QuestionSetSolveMode.STUDY, principal))
			.isInstanceOfSatisfying(QuestionSetStatusException.class,
				ex -> assertThat(ex.getExceptionCode()).isEqualTo(QuestionSetStatusExceptionCode.ONLY_BEFORE));
		assertThat(questionSet.getSolveMode()).isEqualTo(QuestionSetSolveMode.LIVE_TIME);
		assertThat(questionSet.getStatus()).isEqualTo(status);
	}

	@ParameterizedTest
	@EnumSource(QuestionSetSolveMode.class)
	@DisplayName("종료된 두 풀이 방식의 복습 전환은 원래 모드와 풀이 기록을 보존한다")
	void openReview_preservesHistory(QuestionSetSolveMode solveMode) {
		QuestionSetEntity questionSet = saveQuestionSet(QuestionSetStatus.AFTER, solveMode);
		LocalDateTime startedAt = LocalDateTime.now().minusHours(1);
		var session = sessions.save(SolvingSessionEntity.builder().questionSet(questionSet).user(user)
			.solveMode(solveMode).startedAt(startedAt).build());
		session.submit(3, 2);
		entityManager.flush();
		entityManager.clear();

		service.updateQuestionSetToReviewMode(questionSet.getId(), user.getId());
		entityManager.flush();
		entityManager.clear();

		QuestionSetEntity saved = questionSets.findById(questionSet.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(QuestionSetStatus.REVIEW);
		assertThat(saved.getSolveMode()).isEqualTo(solveMode);
		assertThat(saved.getDisplayMode()).isEqualTo(DeliveryMode.REVIEW);
		var savedSession = sessions.findById(session.getId()).orElseThrow();
		assertThat(savedSession.getSolveMode()).isEqualTo(solveMode);
		assertThat(savedSession.getCorrectCount()).isEqualTo(2);
		assertThat(savedSession.getTotalCount()).isEqualTo(3);
	}

	@ParameterizedTest
	@EnumSource(value = QuestionSetStatus.class, names = "AFTER", mode = EnumSource.Mode.EXCLUDE)
	@DisplayName("종료 상태 이외의 복습 전환은 차단한다")
	void openReview_rejectsInvalidStatus(QuestionSetStatus status) {
		QuestionSetEntity questionSet = saveQuestionSet(status, QuestionSetSolveMode.STUDY);
		assertThatThrownBy(() -> service.updateQuestionSetToReviewMode(questionSet.getId(), user.getId()))
			.isInstanceOfSatisfying(QuestionSetStatusException.class,
				ex -> assertThat(ex.getExceptionCode()).isEqualTo(QuestionSetStatusExceptionCode.ONLY_AFTER));
		assertThat(questionSet.getStatus()).isEqualTo(status);
	}

	@ParameterizedTest
	@CsvSource({"BEFORE,PLAYER", "AFTER,PLAYER", "BEFORE,OUTSIDER", "AFTER,OUTSIDER"})
	@DisplayName("모드 변경과 복습 전환은 해당 팀의 Maker 권한을 검증한다")
	void requiresTeamMaker(QuestionSetStatus status, String role) {
		QuestionSetEntity questionSet = saveQuestionSet(status, QuestionSetSolveMode.STUDY);
		var requester = users.save(UserEntity.localLoginUser(UUID.randomUUID() + "@test.com", "pw", "player", null));
		if (role.equals("PLAYER")) {
			members.save(TeamUserEntity.createTeamUser(requester, team, TeamUserRole.PLAYER));
		}
		var requesterPrincipal = MaitUser.builder().id(requester.getId()).build();
		assertThatThrownBy(() -> {
			if (status == QuestionSetStatus.BEFORE) {
				service.changeSolveMode(questionSet.getId(), QuestionSetSolveMode.LIVE_TIME, requesterPrincipal);
			} else {
				service.updateQuestionSetToReviewMode(questionSet.getId(), requester.getId());
			}
		}).isInstanceOf(role.equals("PLAYER") ? UserRoleException.class : EntityNotFoundException.class);
		assertThat(questionSet.getSolveMode()).isEqualTo(QuestionSetSolveMode.STUDY);
		assertThat(questionSet.getStatus()).isEqualTo(status);
		assertThat(questionSet.getTitle()).isEqualTo("유지할 제목");
	}

	@Test
	@DisplayName("개인 팀은 실시간 모드로 변경할 수 없다")
	void personalTeam_cannotChangeToLive() {
		TeamEntity personalTeam = teams.save(TeamEntity.ofPersonal("개인 팀", user.getId()));
		members.save(TeamUserEntity.createTeamUser(user, personalTeam, TeamUserRole.OWNER));
		QuestionSetEntity questionSet = questionSets.save(QuestionSetEntity.builder().teamId(personalTeam.getId())
			.solveMode(QuestionSetSolveMode.STUDY).status(QuestionSetStatus.BEFORE).build());
		assertThatThrownBy(() ->
			service.changeSolveMode(questionSet.getId(), QuestionSetSolveMode.LIVE_TIME, principal))
			.isInstanceOfSatisfying(QuestionSetStatusException.class,
				ex -> assertThat(ex.getExceptionCode())
					.isEqualTo(QuestionSetStatusExceptionCode.CANNOT_CREATE_LIVE_TIME_IN_PERSONAL_TEAM));
	}

	@ParameterizedTest
	@EnumSource(QuestionSetSolveMode.class)
	@DisplayName("제작 완료는 제목·모드를 저장하고 문제 번호를 부여한다")
	void completeQuestionSet_success(QuestionSetSolveMode solveMode) {
		QuestionSetEntity questionSet = saveQuestionSet(QuestionSetStatus.MAKING, solveMode);
		var question = questions.save(ShortQuestionEntity.builder().questionSet(questionSet)
			.content("문제").lexoRank("a").build());
		service.completeQuestionSet(questionSet.getId(), "최종 제목", solveMode, "난이도", List.of());
		entityManager.flush();
		entityManager.clear();
		QuestionSetEntity saved = questionSets.findById(questionSet.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo(QuestionSetStatus.BEFORE);
		assertThat(saved.getSolveMode()).isEqualTo(solveMode);
		assertThat(saved.getTitle()).isEqualTo("최종 제목");
		assertThat(questions.findById(question.getId()).orElseThrow().getNumber()).isEqualTo(1L);
	}

	private QuestionSetEntity saveQuestionSet(QuestionSetStatus status, QuestionSetSolveMode mode) {
		return questionSets.save(QuestionSetEntity.builder().teamId(team.getId()).creatorId(user.getId())
			.title("유지할 제목").difficulty("유지할 난이도").instruction("유지할 설명")
			.status(status).solveMode(mode).build());
	}

	private QuestionSetSolveMode opposite(QuestionSetSolveMode mode) {
		return mode == QuestionSetSolveMode.STUDY ? QuestionSetSolveMode.LIVE_TIME : QuestionSetSolveMode.STUDY;
	}
}
