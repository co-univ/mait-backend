package com.coniv.mait.domain.solve.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.question.entity.MultipleQuestionEntity;
import com.coniv.mait.domain.question.entity.QuestionEntity;
import com.coniv.mait.domain.question.repository.QuestionEntityRepository;
import com.coniv.mait.domain.solve.entity.SolvingSessionEntity;
import com.coniv.mait.domain.solve.entity.StudyAnswerDraftEntity;
import com.coniv.mait.domain.solve.repository.StudyAnswerDraftEntityRepository;

@ExtendWith(MockitoExtension.class)
class StudyAnswerDraftFactoryTest {

	@Mock
	private QuestionEntityRepository questionEntityRepository;

	@Mock
	private StudyAnswerDraftEntityRepository studyAnswerDraftEntityRepository;

	@InjectMocks
	private StudyAnswerDraftFactory studyAnswerDraftFactory;

	@Captor
	private ArgumentCaptor<List<StudyAnswerDraftEntity>> draftsCaptor;

	private final Long solvingSessionId = 1L;
	private final Long questionSetId = 10L;

	@Test
	@DisplayName("문제 셋의 모든 문제에 대해 draft를 생성하고 저장한다")
	void createDrafts_savesAllDrafts() {
		// given
		SolvingSessionEntity solvingSession = mock(SolvingSessionEntity.class);
		when(solvingSession.getId()).thenReturn(solvingSessionId);

		QuestionEntity question1 = mock(MultipleQuestionEntity.class);
		QuestionEntity question2 = mock(MultipleQuestionEntity.class);
		QuestionEntity question3 = mock(MultipleQuestionEntity.class);
		when(question1.getId()).thenReturn(100L);
		when(question2.getId()).thenReturn(101L);
		when(question3.getId()).thenReturn(102L);

		when(questionEntityRepository.findAllByQuestionSetIdOrderByLexoRankAsc(questionSetId))
			.thenReturn(List.of(question1, question2, question3));

		// when
		studyAnswerDraftFactory.createDrafts(solvingSession, questionSetId);

		// then
		verify(studyAnswerDraftEntityRepository).saveAll(draftsCaptor.capture());
		List<StudyAnswerDraftEntity> savedDrafts = draftsCaptor.getValue();

		assertThat(savedDrafts).hasSize(3);
			assertThat(savedDrafts).allSatisfy(draft -> {
				assertThat(draft.getSolvingSession()).isEqualTo(solvingSession);
				Assertions.assertNotNull(draft.getId());
				assertThat(draft.getId().getSolvingSessionId()).isEqualTo(solvingSessionId);
				assertThat(draft.getSubmittedAnswer()).isNull();
				assertThat(draft.isSubmitted()).isFalse();
			});
		assertThat(Objects.requireNonNull(savedDrafts.get(0).getId()).getQuestionId()).isEqualTo(100L);
		assertThat(Objects.requireNonNull(savedDrafts.get(1).getId()).getQuestionId()).isEqualTo(101L);
		assertThat(Objects.requireNonNull(savedDrafts.get(2).getId()).getQuestionId()).isEqualTo(102L);
	}

	@Test
	@DisplayName("문제가 없는 문제 셋이면 빈 리스트로 saveAll을 호출한다")
	void createDrafts_emptyQuestionSet() {
		// given
		SolvingSessionEntity solvingSession = mock(SolvingSessionEntity.class);

		when(questionEntityRepository.findAllByQuestionSetIdOrderByLexoRankAsc(questionSetId))
			.thenReturn(Collections.emptyList());

		// when
		studyAnswerDraftFactory.createDrafts(solvingSession, questionSetId);

		// then
		verify(studyAnswerDraftEntityRepository).saveAll(draftsCaptor.capture());
		assertThat(draftsCaptor.getValue()).isEmpty();
	}

	@Test
	@DisplayName("세션 ID로 draft 목록을 조회한다")
	void getDraftsBySolvingSessionId_returnsDrafts() {
		// given
		StudyAnswerDraftEntity draft1 = mock(StudyAnswerDraftEntity.class);
		StudyAnswerDraftEntity draft2 = mock(StudyAnswerDraftEntity.class);
		when(studyAnswerDraftEntityRepository.findAllByIdSolvingSessionIdOrderByIdQuestionIdAsc(solvingSessionId))
			.thenReturn(List.of(draft1, draft2));

		// when
		List<StudyAnswerDraftEntity> drafts = studyAnswerDraftFactory.getDraftsBySolvingSessionId(solvingSessionId);

		// then
		assertThat(drafts).containsExactly(draft1, draft2);
		verify(studyAnswerDraftEntityRepository).findAllByIdSolvingSessionIdOrderByIdQuestionIdAsc(solvingSessionId);
	}
}
