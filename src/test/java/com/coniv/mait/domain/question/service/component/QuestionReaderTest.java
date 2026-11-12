package com.coniv.mait.domain.question.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionSetEntity;
import com.coniv.mait.domain.question.enums.QuestionSetSolveMode;
import com.coniv.mait.domain.question.enums.QuestionSetStatus;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.question.repository.QuestionSetEntityRepository;
import com.coniv.mait.domain.team.entity.TeamEntity;
import com.coniv.mait.global.exception.custom.ResourceNotBelongException;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class QuestionReaderTest {

	@InjectMocks
	private QuestionReader questionReader;

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private QuestionSetEntityRepository questionSetEntityRepository;

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	@DisplayName("일반 조회와 잠금 조회는 대소문자를 구분하고 동률 키를 ID 순으로 정렬한다")
	void getOrderedQuestions_usesSameRankOrder(boolean forUpdate) {
		// given
		QuestionEntity lower = MultipleQuestionEntity.builder().id(1L).lexoRank("k").build();
		QuestionEntity first = MultipleQuestionEntity.builder().id(2L).lexoRank("V").build();
		QuestionEntity second = MultipleQuestionEntity.builder().id(3L).lexoRank("V").build();
		List<QuestionEntity> unsorted = List.of(lower, second, first);
		if (forUpdate) {
			when(questionEntityRepository.findAllByQuestionSetIdForUpdate(10L)).thenReturn(unsorted);
		} else {
			when(questionEntityRepository.findAllByQuestionSetId(10L)).thenReturn(unsorted);
		}

		// when
		List<QuestionEntity> ordered;
		if (forUpdate) {
			ordered = questionReader.getOrderedQuestionsForUpdate(10L);
		} else {
			ordered = questionReader.getOrderedQuestions(10L);
		}

		// then
		assertThat(ordered).containsExactly(first, second, lower);
	}

	@Test
	@DisplayName("문제 ID와 문제셋 ID로 소속된 문제를 반환한다")
	void getQuestion_validatesMembership() {
		// given
		QuestionSetEntity questionSet = QuestionSetEntity.builder().id(10L).build();
		QuestionEntity question = MultipleQuestionEntity.builder().id(1L).questionSet(questionSet).build();
		when(questionEntityRepository.findById(1L)).thenReturn(Optional.of(question));

		// when & then
		assertThat(questionReader.getQuestion(1L, 10L)).isSameAs(question);
	}

	@Test
	@DisplayName("존재하지 않는 문제는 조회 예외로 처리한다")
	void getQuestion_missing_throwsNotFound() {
		// given
		when(questionEntityRepository.findById(1L)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> questionReader.getQuestion(1L, 10L)).isInstanceOf(EntityNotFoundException.class);
	}

	@Test
	@DisplayName("다른 문제셋의 문제는 소속 오류로 처리한다")
	void getQuestion_differentQuestionSet_throwsNotBelong() {
		// given
		QuestionSetEntity otherSet = QuestionSetEntity.builder().id(20L).build();
		QuestionEntity question = MultipleQuestionEntity.builder().id(1L).questionSet(otherSet).build();
		when(questionEntityRepository.findById(1L)).thenReturn(Optional.of(question));

		// when & then
		assertThatThrownBy(() -> questionReader.getQuestion(1L, 10L)).isInstanceOf(ResourceNotBelongException.class);
	}

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
