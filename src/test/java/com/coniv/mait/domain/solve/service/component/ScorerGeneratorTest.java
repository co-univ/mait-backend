package com.coniv.mait.domain.solve.service.component;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

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

	@Mock
	private RedissonClient redissonClient;

	@Mock
	private RLock lock;

	@InjectMocks
	private ScorerGenerator scorerGenerator;

	private void givenLockAcquired() throws InterruptedException {
		doReturn(lock).when(redissonClient).getLock(anyString());
		doReturn(true).when(lock).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
		doReturn(true).when(lock).isLocked();
		doReturn(true).when(lock).isHeldByCurrentThread();
	}

	@Test
	@DisplayName("득점자 재계산 - 더 빠른 정답자가 기존 득점자를 교체한다")
	void recalculateScorer_fasterCorrectAnswer_replacesExistingScorer() throws InterruptedException {
		// given
		Long questionId = 10L;
		givenLockAcquired();

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
		verify(lock).unlock();
	}

	@Test
	@DisplayName("득점자 재계산 - 기존 득점자가 없으면 새로 저장한다")
	void recalculateScorer_noExistingScorer_insertsNewScorer() throws InterruptedException {
		// given
		Long questionId = 10L;
		givenLockAcquired();

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
		verify(lock).unlock();
	}

	@Test
	@DisplayName("득점자 재계산 - 정답자가 한 명도 없으면 득점자를 제거한다")
	void recalculateScorer_noCorrectAnswer_removesScorer() throws InterruptedException {
		// given
		Long questionId = 10L;
		givenLockAcquired();

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
		verify(lock).unlock();
	}

	@Test
	@DisplayName("득점자 재계산 - 락 획득 실패 시 예외가 발생한다")
	void recalculateScorer_lockAcquisitionFails_throws() throws InterruptedException {
		// given
		Long questionId = 10L;
		doReturn(lock).when(redissonClient).getLock(anyString());
		doReturn(false).when(lock).tryLock(anyLong(), anyLong(), any(TimeUnit.class));

		// when & then
		assertThatThrownBy(() -> scorerGenerator.recalculateScorer(questionId))
			.isInstanceOf(RuntimeException.class)
			.hasMessage("Scorer lock error");

		verify(answerSubmitRecordEntityRepository, never())
			.findFirstByQuestionIdAndIsCorrectTrueOrderBySubmitOrderAsc(anyLong());
		verify(lock, never()).unlock();
	}
}
