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

import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;

@ExtendWith(MockitoExtension.class)
class QuestionReaderTest {

	@InjectMocks
	private QuestionReader questionReader;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@Test
	@DisplayName("완료된 실시간 문제셋의 문제만 반환한다")
	void getCompletedLiveQuestionsInTeam_success() {
		// given
		Long teamId = 1L;
		TeamEntity team = mock(TeamEntity.class);
		QuestionSetEntity questionSet1 = mock(QuestionSetEntity.class);
		QuestionSetEntity questionSet2 = mock(QuestionSetEntity.class);
		QuestionEntity question1 = mock(QuestionEntity.class);
		QuestionEntity question2 = mock(QuestionEntity.class);

		when(team.getId()).thenReturn(teamId);
		when(questionSet1.getId()).thenReturn(10L);
		when(questionSet2.getId()).thenReturn(20L);
		when(questionSetEntityRepository.findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, QuestionSetSolveMode.LIVE_TIME, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW)))
			.thenReturn(List.of(questionSet1, questionSet2));
		when(questionEntityRepository.findAllByQuestionSetIdIn(List.of(10L, 20L)))
			.thenReturn(List.of(question1, question2));

		// when
		List<QuestionEntity> result = questionReader.getCompletedLiveQuestionsInTeam(team);

		// then
		assertThat(result).containsExactly(question1, question2);
		verify(questionSetEntityRepository).findAllByTeamIdAndSolveModeAndStatusIn(
			teamId, QuestionSetSolveMode.LIVE_TIME, List.of(QuestionSetStatus.AFTER, QuestionSetStatus.REVIEW));
	}
}
