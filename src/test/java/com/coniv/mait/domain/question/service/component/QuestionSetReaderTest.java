package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;

@ExtendWith(MockitoExtension.class)
class QuestionSetReaderTest {

	@InjectMocks
	private QuestionSetReader questionSetReader;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Test
	@DisplayName("지정한 풀이 모드이면서 AFTER 또는 REVIEW 상태인 문제셋 목록을 반환한다")
	void getFinishedLiveQuestionSetsBySolveModeInTeam_Success() {
		// given
		Long teamId = 1L;
		QuestionSetSolveMode solveMode = QuestionSetSolveMode.LIVE_TIME;
		QuestionSetEntity qs1 = mock(QuestionSetEntity.class);
		QuestionSetEntity qs2 = mock(QuestionSetEntity.class);

		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, solveMode, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW)))
			.thenReturn(List.of(qs1, qs2));

		// when
		List<QuestionSetEntity> result = questionSetReader.getFinishedLiveQuestionSetsBySolveModeInTeam(teamId,
			solveMode);

		// then
		assertThat(result).hasSize(2);
		verify(questionSetEntityRepository).findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, solveMode, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW));
	}

	@Test
	@DisplayName("조건에 맞는 문제셋이 없으면 빈 리스트를 반환한다")
	void getFinishedLiveQuestionSetsBySolveModeInTeam_Empty() {
		// given
		Long teamId = 1L;
		QuestionSetSolveMode solveMode = QuestionSetSolveMode.LIVE_TIME;

		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, solveMode, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW)))
			.thenReturn(List.of());

		// when
		List<QuestionSetEntity> result = questionSetReader.getFinishedLiveQuestionSetsBySolveModeInTeam(teamId,
			solveMode);

		// then
		assertThat(result).isEmpty();
	}
}
