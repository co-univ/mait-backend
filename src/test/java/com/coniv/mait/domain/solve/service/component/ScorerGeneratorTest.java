package com.coniv.mait.domain.solve.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.solve.entity.AnswerSubmitRecordEntity;
import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;

@ExtendWith(MockitoExtension.class)
class ScorerGeneratorTest {

	@Mock
	private QuestionScorerEntityRepository questionScorerEntityRepository;

	@Mock
	private AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	@Mock
	private ScorerProcessor scorerProcessor;

	@InjectMocks
	private ScorerGenerator scorerGenerator;

	@Test
	@DisplayName("득점자 재계산 - 더 빠른 정답자가 기존 득점자를 교체한다")
	void recalculateScorer_fasterCorrectAnswer_replacesExistingScorer() {
		// given
		Long questionId = 10L;

		AnswerSubmitRecordEntity topCorrect = mock(AnswerSubmitRecordEntity.class);
		doReturn(2L).when(topCorrect).getUserId();
		doReturn(3L).when(topCorrect).getSubmitOrder();
		doReturn(Optional.of(topCorrect)).when(answerSubmitRecordEntityRepository)
			.findFirstByQuestionIdAndIsCorrectTrueOrderBySubmitOrderAsc(questionId);

		QuestionScorerEntity existingScorer = mock(QuestionScorerEntity.class);
		doReturn(Optional.of(existingScorer)).when(questionScorerEntityRepository).findByQuestionId(questionId);

		// when
		scorerGenerator.recalculateScorer(questionId);

		// then
		verify(existingScorer).updateScorer(2L, 3L);
		verify(questionScorerEntityRepository).save(existingScorer);
		verify(scorerProcessor).setScorer(questionId, 2L, 3L);
	}

	@Test
	@DisplayName("득점자 재계산 - 기존 득점자가 없으면 새로 저장한다")
	void recalculateScorer_noExistingScorer_insertsNewScorer() {
		// given
		Long questionId = 10L;

		AnswerSubmitRecordEntity topCorrect = mock(AnswerSubmitRecordEntity.class);
		doReturn(5L).when(topCorrect).getUserId();
		doReturn(1L).when(topCorrect).getSubmitOrder();
		doReturn(Optional.of(topCorrect)).when(answerSubmitRecordEntityRepository)
			.findFirstByQuestionIdAndIsCorrectTrueOrderBySubmitOrderAsc(questionId);

		doReturn(Optional.empty()).when(questionScorerEntityRepository).findByQuestionId(questionId);

		// when
		scorerGenerator.recalculateScorer(questionId);

		// then
		ArgumentCaptor<QuestionScorerEntity> captor = ArgumentCaptor.forClass(QuestionScorerEntity.class);
		verify(questionScorerEntityRepository).save(captor.capture());
		QuestionScorerEntity saved = captor.getValue();
		assertThat(saved.getQuestionId()).isEqualTo(questionId);
		assertThat(saved.getUserId()).isEqualTo(5L);
		assertThat(saved.getSubmitOrder()).isEqualTo(1L);

		verify(scorerProcessor).setScorer(questionId, 5L, 1L);
	}

	@Test
	@DisplayName("득점자 재계산 - 정답자가 한 명도 없으면 득점자를 제거한다")
	void recalculateScorer_noCorrectAnswer_removesScorer() {
		// given
		Long questionId = 10L;

		doReturn(Optional.empty()).when(answerSubmitRecordEntityRepository)
			.findFirstByQuestionIdAndIsCorrectTrueOrderBySubmitOrderAsc(questionId);

		QuestionScorerEntity existingScorer = mock(QuestionScorerEntity.class);
		doReturn(Optional.of(existingScorer)).when(questionScorerEntityRepository).findByQuestionId(questionId);

		// when
		scorerGenerator.recalculateScorer(questionId);

		// then
		verify(questionScorerEntityRepository).delete(existingScorer);
		verify(scorerProcessor).clearScorer(questionId);
		verify(scorerProcessor, never()).setScorer(anyLong(), anyLong(), anyLong());
	}

	@Test
	@DisplayName("득점자 갱신 - 기존 득점자가 없으면 새로 저장한다")
	void updateScorer_noExistingScorer_insertsNewScorer() {
		// given
		Long questionId = 10L;
		doReturn(Optional.empty()).when(questionScorerEntityRepository).findByQuestionId(questionId);

		// when
		scorerGenerator.updateScorer(questionId, 7L, 2L);

		// then
		ArgumentCaptor<QuestionScorerEntity> captor = ArgumentCaptor.forClass(QuestionScorerEntity.class);
		verify(questionScorerEntityRepository).save(captor.capture());
		assertThat(captor.getValue())
			.extracting(QuestionScorerEntity::getQuestionId, QuestionScorerEntity::getUserId,
				QuestionScorerEntity::getSubmitOrder)
			.containsExactly(questionId, 7L, 2L);
	}

	@Test
	@DisplayName("득점자 갱신 - 더 빠른 제출이면 기존 득점자를 교체한다")
	void updateScorer_fasterSubmit_replacesExistingScorer() {
		// given
		Long questionId = 10L;
		QuestionScorerEntity existingScorer = mock(QuestionScorerEntity.class);
		doReturn(5L).when(existingScorer).getSubmitOrder();
		doReturn(Optional.of(existingScorer)).when(questionScorerEntityRepository).findByQuestionId(questionId);

		// when
		scorerGenerator.updateScorer(questionId, 7L, 2L);

		// then
		verify(existingScorer).updateScorer(7L, 2L);
		verify(questionScorerEntityRepository).save(existingScorer);
	}

	@Test
	@DisplayName("득점자 갱신 - 기존 득점자가 더 빠르면 유지한다")
	void updateScorer_slowerSubmit_keepsExistingScorer() {
		// given
		Long questionId = 10L;
		QuestionScorerEntity existingScorer = mock(QuestionScorerEntity.class);
		doReturn(1L).when(existingScorer).getSubmitOrder();
		doReturn(Optional.of(existingScorer)).when(questionScorerEntityRepository).findByQuestionId(questionId);

		// when
		scorerGenerator.updateScorer(questionId, 7L, 2L);

		// then
		verify(existingScorer, never()).updateScorer(anyLong(), anyLong());
		verify(questionScorerEntityRepository, never()).save(any());
	}
}
