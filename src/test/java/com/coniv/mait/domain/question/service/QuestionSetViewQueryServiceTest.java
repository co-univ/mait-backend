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
import com.coniv.mait.domain.question.entity.QuestionSetParticipantEntity;
import com.coniv.mait.domain.question.enums.ParticipantStatus;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetParticipantRepository;
import com.coniv.mait.domain.solve.service.StudyModeService;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.web.question.dto.QuestionSetGroupBy;
import com.coniv.mait.web.question.dto.QuestionSetView;
import com.coniv.mait.web.question.dto.QuestionSetViewApiResponse;
import com.coniv.mait.web.question.dto.QuestionSetViewType;

@ExtendWith(MockitoExtension.class)
class QuestionSetViewQueryServiceTest {

	private static final Long USER_ID = 10L;
	private static final Long TEAM_ID = 1L;

	@InjectMocks
	private QuestionSetViewQueryService questionSetViewQueryService;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private QuestionSetParticipantRepository questionSetParticipantRepository;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private StudyModeService studyModeService;

	@Test
	@DisplayName("실시간 풀이 목록은 현재 유저의 참여 상태로 그룹화한다")
	void getLiveSolvingQuestionSets_GroupsByUserParticipationStatus() {
		// given
		MaitUser user = MaitUser.builder().id(USER_ID).build();
		QuestionSetEntity notParticipated = questionSet(1L, QuestionSetStatus.ONGOING, LocalDateTime.now());
		QuestionSetEntity active = questionSet(2L, QuestionSetStatus.ONGOING, LocalDateTime.now().minusMinutes(1));
		QuestionSetEntity eliminated = questionSet(3L, QuestionSetStatus.ONGOING, LocalDateTime.now().minusMinutes(2));
		QuestionSetEntity finished = questionSet(4L, QuestionSetStatus.AFTER, LocalDateTime.now().minusMinutes(3));
		QuestionSetParticipantEntity activeParticipant = participant(100L, active, ParticipantStatus.ACTIVE);
		QuestionSetParticipantEntity eliminatedParticipant =
			participant(101L, eliminated, ParticipantStatus.ELIMINATED);
		QuestionSetParticipantEntity finishedParticipant = participant(102L, finished, ParticipantStatus.ACTIVE);

		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			eq(TEAM_ID),
			eq(QuestionSetSolveMode.LIVE_TIME),
			anyList())).thenReturn(List.of(notParticipated, active, eliminated, finished));
		when(questionSetParticipantRepository.findAllByUserIdAndQuestionSetTeamId(USER_ID, TEAM_ID))
			.thenReturn(List.of(activeParticipant, eliminatedParticipant, finishedParticipant));

		// when
		QuestionSetViewApiResponse response = questionSetViewQueryService.getLiveSolvingQuestionSets(
			TEAM_ID,
			user);

		// then
		assertThat(response.view()).isEqualTo(QuestionSetView.SOLVING);
		assertThat(response.type()).isEqualTo(QuestionSetViewType.LIVE_TIME);
		assertThat(response.groupBy()).isEqualTo(QuestionSetGroupBy.USER_PARTICIPATION_STATUS);
		assertThat(response.sections()).hasSize(4);
		assertThat(response.sections().get(0).key()).isEqualTo("NOT_PARTICIPATED");
		assertThat(response.sections().get(0).items()).extracting("id").containsExactly(1L);
		assertThat(response.sections().get(0).items()).extracting("participated").containsExactly(false);
		assertThat(response.sections().get(1).key()).isEqualTo("PARTICIPATING");
		assertThat(response.sections().get(1).items()).extracting("id").containsExactly(2L);
		assertThat(response.sections().get(1).items()).extracting("participantStatus").containsExactly(
			ParticipantStatus.ACTIVE);
		assertThat(response.sections().get(2).key()).isEqualTo("ELIMINATED");
		assertThat(response.sections().get(2).items()).extracting("id").containsExactly(3L);
		assertThat(response.sections().get(2).items()).extracting("participantStatus").containsExactly(
			ParticipantStatus.ELIMINATED);
		assertThat(response.sections().get(3).key()).isEqualTo("FINISHED");
		assertThat(response.sections().get(3).items()).extracting("id").containsExactly(4L);
		assertThat(response.sections().get(3).items()).extracting("participated").containsExactly(true);

		verify(teamRoleValidator).checkIsTeamMember(TEAM_ID, USER_ID);
	}

	@Test
	@DisplayName("학습 관리 목록은 문제 셋 상태로 그룹화한다")
	void getStudyManagementQuestionSets_GroupsByQuestionSetStatus() {
		// given
		MaitUser user = MaitUser.builder().id(USER_ID).build();
		QuestionSetEntity before = questionSet(1L, QuestionSetStatus.BEFORE, LocalDateTime.now());
		QuestionSetEntity ongoing = questionSet(2L, QuestionSetStatus.ONGOING, LocalDateTime.now().minusMinutes(1));

		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			eq(TEAM_ID),
			eq(QuestionSetSolveMode.STUDY),
			anyList())).thenReturn(List.of(before, ongoing));

		// when
		QuestionSetViewApiResponse response = questionSetViewQueryService.getStudyManagementQuestionSets(
			TEAM_ID,
			user);

		// then
		assertThat(response.view()).isEqualTo(QuestionSetView.MANAGEMENT);
		assertThat(response.type()).isEqualTo(QuestionSetViewType.STUDY);
		assertThat(response.groupBy()).isEqualTo(QuestionSetGroupBy.QUESTION_SET_STATUS);
		assertThat(response.sections()).hasSize(3);
		assertThat(response.sections().get(0).key()).isEqualTo("BEFORE");
		assertThat(response.sections().get(0).items()).extracting("id").containsExactly(1L);
		assertThat(response.sections().get(1).key()).isEqualTo("ONGOING");
		assertThat(response.sections().get(1).items()).extracting("id").containsExactly(2L);
		assertThat(response.sections().get(2).key()).isEqualTo("AFTER");
		assertThat(response.sections().get(2).items()).isEmpty();

		verify(teamRoleValidator).checkHasCreateQuestionSetAuthority(TEAM_ID, USER_ID);
	}

	private QuestionSetEntity questionSet(final Long id, final QuestionSetStatus status,
		final LocalDateTime modifiedAt) {
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);
		when(questionSet.getId()).thenReturn(id);
		when(questionSet.getStatus()).thenReturn(status);
		when(questionSet.getTeamId()).thenReturn(TEAM_ID);
		when(questionSet.getSolveMode()).thenReturn(QuestionSetSolveMode.LIVE_TIME);
		when(questionSet.getModifiedAt()).thenReturn(modifiedAt);
		return questionSet;
	}

	private QuestionSetParticipantEntity participant(final Long id, final QuestionSetEntity questionSet,
		final ParticipantStatus status) {
		QuestionSetParticipantEntity participant = mock(QuestionSetParticipantEntity.class);
		when(participant.getId()).thenReturn(id);
		when(participant.getQuestionSet()).thenReturn(questionSet);
		when(participant.getStatus()).thenReturn(status);
		return participant;
	}
}
