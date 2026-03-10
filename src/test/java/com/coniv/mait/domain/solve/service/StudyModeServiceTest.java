package com.coniv.mait.domain.solve.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftId;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.solve.service.component.StudyAnswerDraftFactory;
import com.coniv.mait.domain.solve.service.dto.StudyAnswerDraftDto;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.global.auth.model.MaitUser;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class StudyModeServiceTest {

	private static final Long USER_ID = 1L;
	private static final Long QUESTION_SET_ID = 10L;
	private static final Long TEAM_ID = 100L;
	private static final MaitUser MAIT_USER = MaitUser.builder().id(USER_ID).build();

	@InjectMocks
	private StudyModeService studyModeService;

	@Mock
	private UserReader userReader;

	@Mock
	private TeamRoleValidator teamRoleValidator;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	@Mock
	private StudyAnswerDraftFactory studyAnswerDraftFactory;

	@Test
	@DisplayName("startStudyMode - 기존 세션이 없으면 새 세션을 생성하고 draft를 생성한다")
	void startStudyMode_createNewSession_WhenNoExistingSession() {
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);
		SolvingSessionEntity savedSession = mock(SolvingSessionEntity.class);

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndMode(USER_ID, QUESTION_SET_ID,
			DeliveryMode.STUDY)).thenReturn(Optional.empty());
		when(solvingSessionEntityRepository.save(any(SolvingSessionEntity.class))).thenReturn(savedSession);
		when(savedSession.getUser()).thenReturn(mockUser);
		when(savedSession.getQuestionSet()).thenReturn(mockQuestionSet);

		studyModeService.startStudyMode(MAIT_USER, QUESTION_SET_ID);

		verify(solvingSessionEntityRepository).save(any(SolvingSessionEntity.class));
		verify(studyAnswerDraftFactory).createDrafts(savedSession, QUESTION_SET_ID);
	}

	@Test
	@DisplayName("startStudyMode - 기존 세션이 있으면 저장 없이 바로 반환한다")
	void startStudyMode_returnExistingSession_WhenSessionAlreadyExists() {
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);
		SolvingSessionEntity existingSession = mock(SolvingSessionEntity.class);

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndMode(USER_ID, QUESTION_SET_ID,
			DeliveryMode.STUDY)).thenReturn(Optional.of(existingSession));
		when(existingSession.getUser()).thenReturn(mockUser);
		when(existingSession.getQuestionSet()).thenReturn(mockQuestionSet);

		studyModeService.startStudyMode(MAIT_USER, QUESTION_SET_ID);

		verify(solvingSessionEntityRepository, never()).save(any());
		verify(studyAnswerDraftFactory, never()).createDrafts(any(), any());
	}

	@Test
	@DisplayName("startStudyMode - 존재하지 않는 문제 셋이면 EntityNotFoundException이 발생한다")
	void startStudyMode_throwException_WhenQuestionSetNotFound() {
		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> studyModeService.startStudyMode(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class);

		verify(solvingSessionEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("startStudyMode - 존재하지 않는 유저이면 EntityNotFoundException이 발생한다")
	void startStudyMode_throwException_WhenUserNotFound() {
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenThrow(new EntityNotFoundException("해당 유저를 찾을 수 없습니다."));

		assertThatThrownBy(() -> studyModeService.startStudyMode(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class);

		verify(solvingSessionEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("startStudyMode - 팀 내 문제 풀기 권한이 없으면 UserRoleException이 발생한다")
	void startStudyMode_throwException_WhenNoAuthorityInTeam() {
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		doThrow(new UserRoleException("해당 문제를 풀 수 있는 권한이 없습니다."))
			.when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(TEAM_ID, USER_ID);

		assertThatThrownBy(() -> studyModeService.startStudyMode(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(UserRoleException.class);

		verify(solvingSessionEntityRepository, never()).save(any());
	}

	@Test
	@DisplayName("getStudyAnswerDrafts - 학습 세션의 draft 목록을 DTO로 반환한다")
	void getStudyAnswerDrafts_returnsDraftDtos() {
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);
		SolvingSessionEntity existingSession = mock(SolvingSessionEntity.class);

		Long solvingSessionId = 1000L;
		StudyAnswerDraftEntity draft1 = StudyAnswerDraftEntity.builder()
			.id(new StudyAnswerDraftId(solvingSessionId, 101L))
			.submittedAnswer("{\"a\":1}")
			.submitted(true)
			.build();
		StudyAnswerDraftEntity draft2 = StudyAnswerDraftEntity.builder()
			.id(new StudyAnswerDraftId(solvingSessionId, 102L))
			.submittedAnswer(null)
			.submitted(false)
			.build();

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndMode(USER_ID, QUESTION_SET_ID,
			DeliveryMode.STUDY)).thenReturn(Optional.of(existingSession));
		when(existingSession.getId()).thenReturn(solvingSessionId);
		when(studyAnswerDraftFactory.getDraftsBySolvingSessionId(solvingSessionId)).thenReturn(List.of(draft1, draft2));

		List<StudyAnswerDraftDto> result = studyModeService.getStudyAnswerDrafts(MAIT_USER, QUESTION_SET_ID);

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().getSolvingSessionId()).isEqualTo(solvingSessionId);
		assertThat(result.getFirst().getQuestionId()).isEqualTo(101L);
		assertThat(result.get(0).getSubmittedAnswer()).isEqualTo("{\"a\":1}");
		assertThat(result.get(0).isSubmitted()).isTrue();
		assertThat(result.get(1).getQuestionId()).isEqualTo(102L);
		assertThat(result.get(1).getSubmittedAnswer()).isNull();
		assertThat(result.get(1).isSubmitted()).isFalse();
	}

	@Test
	@DisplayName("getStudyAnswerDrafts - 학습 세션이 없으면 EntityNotFoundException이 발생한다")
	void getStudyAnswerDrafts_throwsException_WhenSessionNotFound() {
		QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
		UserEntity mockUser = mock(UserEntity.class);

		when(questionSetEntityRepository.findById(QUESTION_SET_ID)).thenReturn(Optional.of(mockQuestionSet));
		when(userReader.getById(USER_ID)).thenReturn(mockUser);
		when(mockQuestionSet.getTeamId()).thenReturn(TEAM_ID);
		when(mockUser.getId()).thenReturn(USER_ID);
		when(mockQuestionSet.getId()).thenReturn(QUESTION_SET_ID);
		when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndMode(USER_ID, QUESTION_SET_ID,
			DeliveryMode.STUDY)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> studyModeService.getStudyAnswerDrafts(MAIT_USER, QUESTION_SET_ID))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("존재하지 않는 학습 세션 입니다.");
	}
}
