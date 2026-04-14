package com.coniv.mait.domain.question.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.enums.UserStudyStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.web.question.dto.StudyQuestionSetGroup;

@ExtendWith(MockitoExtension.class)
class StudyQuestionSetQueryServiceTest {

	private static final Long USER_ID = 10L;

	@InjectMocks
	private StudyQuestionSetQueryService studyQuestionSetQueryService;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Test
	@DisplayName("STUDY 모드 문제 셋 목록 조회 - 유저 풀이 세션 기준으로 상태를 그룹화한다")
	void getStudyQuestionSets_GroupsByUserSolvingSession() {
		// given
		final Long teamId = 1L;
		final LocalDateTime now = LocalDateTime.now();
		final MaitUser user = MaitUser.builder().id(USER_ID).build();

		QuestionSetEntity notStartedEntity = mockStudyQuestionSet(1L, teamId, QuestionSetStatus.BEFORE,
			now.minusDays(2));
		QuestionSetEntity progressingEntity = mockStudyQuestionSet(2L, teamId, QuestionSetStatus.BEFORE,
			now.minusDays(1));
		QuestionSetEntity completedEntity = mockStudyQuestionSet(3L, teamId, QuestionSetStatus.BEFORE, now);

		when(questionSetEntityRepository.findAllByTeamId(teamId))
			.thenReturn(List.of(notStartedEntity, progressingEntity, completedEntity));

		SolvingSessionEntity progressingSession = mockSession(100L, 2L, SolvingStatus.PROGRESSING);
		SolvingSessionEntity completedSession = mockSession(101L, 3L, SolvingStatus.COMPLETE);

		when(solvingSessionEntityRepository.findAllByUserIdAndModeAndQuestionSetTeamId(
			USER_ID, DeliveryMode.STUDY, teamId))
			.thenReturn(List.of(progressingSession, completedSession));

		// when
		StudyQuestionSetGroup result = studyQuestionSetQueryService.getStudyQuestionSets(teamId, user);

		// then
		assertThat(result.questionSets().get(UserStudyStatus.BEFORE)).hasSize(1);
		assertThat(result.questionSets().get(UserStudyStatus.BEFORE).get(0).getId()).isEqualTo(1L);
		assertThat(result.questionSets().get(UserStudyStatus.BEFORE).get(0).getSolvingSessionId()).isNull();

		assertThat(result.questionSets().get(UserStudyStatus.ONGOING)).hasSize(1);
		assertThat(result.questionSets().get(UserStudyStatus.ONGOING).get(0).getId()).isEqualTo(2L);
		assertThat(result.questionSets().get(UserStudyStatus.ONGOING).get(0).getSolvingSessionId()).isEqualTo(100L);

		assertThat(result.questionSets().get(UserStudyStatus.AFTER)).hasSize(1);
		assertThat(result.questionSets().get(UserStudyStatus.AFTER).get(0).getId()).isEqualTo(3L);
		assertThat(result.questionSets().get(UserStudyStatus.AFTER).get(0).getSolvingSessionId()).isEqualTo(101L);

		verify(teamRoleValidator).checkIsTeamMember(teamId, USER_ID);
		verify(solvingSessionEntityRepository)
			.findAllByUserIdAndModeAndQuestionSetTeamId(USER_ID, DeliveryMode.STUDY, teamId);
	}

	@Test
	@DisplayName("STUDY 모드 문제 셋 목록 조회 - 유저 세션이 없으면 BEFORE로 표시한다")
	void getStudyQuestionSets_NoSession_DisplaysAsBefore() {
		// given
		final Long teamId = 1L;
		final MaitUser user = MaitUser.builder().id(USER_ID).build();
		QuestionSetEntity entity = mockStudyQuestionSet(1L, teamId, QuestionSetStatus.ONGOING, LocalDateTime.now());

		when(questionSetEntityRepository.findAllByTeamId(teamId)).thenReturn(List.of(entity));
		when(solvingSessionEntityRepository.findAllByUserIdAndModeAndQuestionSetTeamId(
			USER_ID, DeliveryMode.STUDY, teamId))
			.thenReturn(List.of());

		// when
		StudyQuestionSetGroup result = studyQuestionSetQueryService.getStudyQuestionSets(teamId, user);

		// then
		assertThat(result.questionSets()).containsKey(UserStudyStatus.BEFORE);
		assertThat(result.questionSets().get(UserStudyStatus.BEFORE)).hasSize(1);
		assertThat(result.questionSets().get(UserStudyStatus.BEFORE).get(0).getId()).isEqualTo(1L);
		assertThat(result.questionSets().get(UserStudyStatus.ONGOING)).isEmpty();
		assertThat(result.questionSets().get(UserStudyStatus.AFTER)).isEmpty();
	}

	private QuestionSetEntity mockStudyQuestionSet(final Long id, final Long teamId, final QuestionSetStatus status,
		final LocalDateTime modifiedAt) {
		QuestionSetEntity entity = mock(QuestionSetEntity.class);
		when(entity.getId()).thenReturn(id);
		when(entity.getDisplayMode()).thenReturn(DeliveryMode.STUDY);
		when(entity.getStatus()).thenReturn(status);
		when(entity.getTeamId()).thenReturn(teamId);
		when(entity.getModifiedAt()).thenReturn(modifiedAt);
		return entity;
	}

	private SolvingSessionEntity mockSession(final Long id, final Long questionSetId, final SolvingStatus status) {
		SolvingSessionEntity session = mock(SolvingSessionEntity.class);
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(session.getId()).thenReturn(id);
		when(session.getStatus()).thenReturn(status);
		when(session.getQuestionSet()).thenReturn(questionSet);
		when(questionSet.getId()).thenReturn(questionSetId);
		return session;
	}
}
