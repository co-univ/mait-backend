package com.coniv.mait.domain.solve.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.solve.enums.SolvingStatus;
import com.coniv.mait.domain.solve.repository.SolvingSessionEntityRepository;

@ExtendWith(MockitoExtension.class)
class SolvingSessionReaderTest {

	@Mock
	private SolvingSessionEntityRepository solvingSessionEntityRepository;

	@InjectMocks
	private SolvingSessionReader solvingSessionReader;

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	@DisplayName("사용자와 문제셋을 기준으로 완료된 학습 세션의 존재 여부를 반환한다")
	void hasCompletedStudySession_returnsWhetherCompletedSessionExists(boolean exists) {
		// given
		final Long userId = 1L;
		final Long questionSetId = 10L;
		when(solvingSessionEntityRepository.existsByUserIdAndQuestionSetIdAndSolveModeAndStatus(
			userId, questionSetId, QuestionSetSolveMode.STUDY, SolvingStatus.COMPLETE)).thenReturn(exists);

		// when
		boolean completed = solvingSessionReader.hasCompletedStudySession(userId, questionSetId);

		// then
		assertThat(completed).isEqualTo(exists);
	}
}
