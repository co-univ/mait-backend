package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.team.exception.TeamManagerException;
import com.coniv.mait.domain.team.service.component.TeamReader;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionSetReaderTest {

	private enum GuardMethod {
		GET_ACTIVE {
			@Override
			void invoke(final QuestionSetReader questionSetReader, final Long questionSetId) {
				questionSetReader.getActiveQuestionSet(questionSetId);
			}
		},
		VALIDATE_ACTIVE {
			@Override
			void invoke(final QuestionSetReader questionSetReader, final Long questionSetId) {
				questionSetReader.validateActiveQuestionSet(questionSetId);
			}
		};

		abstract void invoke(QuestionSetReader questionSetReader, Long questionSetId);
	}

	@InjectMocks
	private QuestionSetReader questionSetReader;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Mock
	private TeamReader teamReader;

	@Test
	@DisplayName("LIVE_TIME 모드이면서 AFTER 상태인 문제셋 목록을 반환한다")
	void getFinishedLiveQuestionSetsInTeam_Success() {
		// given
		Long teamId = 1L;
		QuestionSetEntity qs1 = mock(QuestionSetEntity.class);
		QuestionSetEntity qs2 = mock(QuestionSetEntity.class);

		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, QuestionSetSolveMode.LIVE_TIME, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW)))
			.thenReturn(List.of(qs1, qs2));

		// when
		List<QuestionSetEntity> result = questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId);

		// then
		assertThat(result).hasSize(2);
		verify(questionSetEntityRepository).findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, QuestionSetSolveMode.LIVE_TIME, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW));
	}

	@Test
	@DisplayName("조건에 맞는 문제셋이 없으면 빈 리스트를 반환한다")
	void getFinishedLiveQuestionSetsInTeam_Empty() {
		// given
		Long teamId = 1L;

		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, QuestionSetSolveMode.LIVE_TIME, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW)))
			.thenReturn(List.of());

		// when
		List<QuestionSetEntity> result = questionSetReader.getFinishedLiveQuestionSetsInTeam(teamId);

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("활성 문제셋 조회 성공 시 팀 활성 여부도 함께 검증한다")
	void getActiveQuestionSet_Success() {
		// given
		Long questionSetId = 1L;
		Long teamId = 2L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSet.getTeamId()).thenReturn(teamId);

		// when
		QuestionSetEntity result = questionSetReader.getActiveQuestionSet(questionSetId);

		// then
		assertThat(result).isSameAs(questionSet);
		verify(questionSetEntityRepository).findById(questionSetId);
		verify(teamReader).getActiveTeam(teamId);
	}

	@ParameterizedTest(name = "{0} - 삭제된 팀 문제셋이면 예외를 던진다")
	@EnumSource(GuardMethod.class)
	void activeQuestionSetGuard_DeletedTeam_ThrowsException(final GuardMethod guardMethod) {
		// given
		Long questionSetId = 1L;
		Long teamId = 2L;
		QuestionSetEntity questionSet = mock(QuestionSetEntity.class);

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.of(questionSet));
		when(questionSet.getTeamId()).thenReturn(teamId);
		when(teamReader.getActiveTeam(teamId)).thenThrow(new TeamManagerException("삭제된 팀입니다."));

		// when & then
		assertThatThrownBy(() -> guardMethod.invoke(questionSetReader, questionSetId))
			.isInstanceOf(TeamManagerException.class)
			.hasMessage("삭제된 팀입니다.");

		verify(questionSetEntityRepository).findById(questionSetId);
		verify(teamReader).getActiveTeam(teamId);
	}

	@ParameterizedTest(name = "{0} - 존재하지 않는 문제셋이면 예외를 던진다")
	@EnumSource(GuardMethod.class)
	void activeQuestionSetGuard_NotFound_ThrowsException(final GuardMethod guardMethod) {
		// given
		Long questionSetId = 999L;

		when(questionSetEntityRepository.findById(questionSetId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> guardMethod.invoke(questionSetReader, questionSetId))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessage("해당 문제 셋을 찾을 수 없습니다.");

		verify(questionSetEntityRepository).findById(questionSetId);
		verifyNoInteractions(teamReader);
	}
}
