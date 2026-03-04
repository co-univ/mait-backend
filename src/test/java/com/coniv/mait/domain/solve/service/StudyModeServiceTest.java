package com.coniv.mait.domain.solve.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.DeliveryMode;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.exception.UserRoleException;
import com.coniv.mait.domain.user.service.component.TeamRoleValidator;
import com.coniv.mait.domain.user.service.component.UserReader;
import com.coniv.mait.global.auth.model.MaitUser;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class StudyModeServiceTest {

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

	@Nested
	@DisplayName("startStudyMode 메서드")
	class StartStudyMode {

		private final Long userId = 1L;
		private final Long questionSetId = 10L;
		private final Long teamId = 100L;
		private final MaitUser maitUser = MaitUser.builder().id(userId).build();

		@Test
		@DisplayName("기존 세션이 없으면 새 세션을 생성하고 저장한다")
		void createNewSession_WhenNoExistingSession() {
			// given
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
			UserEntity mockUser = mock(UserEntity.class);
			SolvingSessionEntity savedSession = mock(SolvingSessionEntity.class);

			when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(mockQuestionSet));
			when(userReader.getById(userId)).thenReturn(mockUser);
			when(mockQuestionSet.getTeamId()).thenReturn(teamId);
			when(mockUser.getId()).thenReturn(userId);
			when(mockQuestionSet.getId()).thenReturn(questionSetId);
			doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);
			when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndMode(userId, questionSetId,
				DeliveryMode.STUDY)).thenReturn(Optional.empty());
			when(solvingSessionEntityRepository.save(any(SolvingSessionEntity.class))).thenReturn(savedSession);

			// when
			studyModeService.startStudyMode(maitUser, questionSetId);

			// then
			verify(solvingSessionEntityRepository).save(any(SolvingSessionEntity.class));
		}

		@Test
		@DisplayName("기존 세션이 있으면 해당 세션을 그대로 저장한다")
		void saveExistingSession_WhenSessionAlreadyExists() {
			// given
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
			UserEntity mockUser = mock(UserEntity.class);
			SolvingSessionEntity existingSession = mock(SolvingSessionEntity.class);

			when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(mockQuestionSet));
			when(userReader.getById(userId)).thenReturn(mockUser);
			when(mockQuestionSet.getTeamId()).thenReturn(teamId);
			when(mockUser.getId()).thenReturn(userId);
			when(mockQuestionSet.getId()).thenReturn(questionSetId);
			doNothing().when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);
			when(solvingSessionEntityRepository.findByUserIdAndQuestionSetIdAndMode(userId, questionSetId,
				DeliveryMode.STUDY)).thenReturn(Optional.of(existingSession));
			when(solvingSessionEntityRepository.save(existingSession)).thenReturn(existingSession);

			// when
			studyModeService.startStudyMode(maitUser, questionSetId);

			// then
			verify(solvingSessionEntityRepository).save(existingSession);
		}

		@Test
		@DisplayName("존재하지 않는 문제 셋이면 EntityNotFoundException이 발생한다")
		void throwException_WhenQuestionSetNotFound() {
			// given
			when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> studyModeService.startStudyMode(maitUser, questionSetId))
				.isInstanceOf(EntityNotFoundException.class);

			verify(solvingSessionEntityRepository, never()).save(any());
		}

		@Test
		@DisplayName("존재하지 않는 유저이면 EntityNotFoundException이 발생한다")
		void throwException_WhenUserNotFound() {
			// given
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);

			when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(mockQuestionSet));
			when(userReader.getById(userId)).thenThrow(new EntityNotFoundException("해당 유저를 찾을 수 없습니다."));

			// when & then
			assertThatThrownBy(() -> studyModeService.startStudyMode(maitUser, questionSetId))
				.isInstanceOf(EntityNotFoundException.class);

			verify(solvingSessionEntityRepository, never()).save(any());
		}

		@Test
		@DisplayName("팀 내 문제 풀기 권한이 없으면 UserRoleException이 발생한다")
		void throwException_WhenNoAuthorityInTeam() {
			// given
			QuestionSetEntity mockQuestionSet = mock(QuestionSetEntity.class);
			UserEntity mockUser = mock(UserEntity.class);

			when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(mockQuestionSet));
			when(userReader.getById(userId)).thenReturn(mockUser);
			when(mockQuestionSet.getTeamId()).thenReturn(teamId);
			when(mockUser.getId()).thenReturn(userId);
			doThrow(new UserRoleException("해당 문제를 풀 수 있는 권한이 없습니다."))
				.when(teamRoleValidator).checkHasSolveQuestionAuthorityInTeam(teamId, userId);

			// when & then
			assertThatThrownBy(() -> studyModeService.startStudyMode(maitUser, questionSetId))
				.isInstanceOf(UserRoleException.class);

			verify(solvingSessionEntityRepository, never()).save(any());
		}
	}
}
