package com.coniv.mait.domain.solve.service.component;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.AnswerSubmitRecordEntityRepository;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;
import com.coniv.mait.global.lock.DistributedLock;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScorerGenerator {

	// 득점자 판정은 제출 순서대로 처리돼야 하므로 즉시 거절하지 않고 대기한다.
	private static final long LOCK_WAIT_SECONDS = 30L;

	private static final String LOCK_KEY = "'scorer:update:questionId:' + #questionId";

	private final QuestionScorerEntityRepository questionScorerEntityRepository;

	private final AnswerSubmitRecordEntityRepository answerSubmitRecordEntityRepository;

	private final ScorerProcessor scorerProcessor;

	// 제출 트랜잭션에 합류해 득점자와 제출 기록을 함께 커밋한다.
	// 락은 호출자 트랜잭션이 끝날 때 해제되므로 재계산이 미커밋 제출을 놓치지 않는다.
	@DistributedLock(key = LOCK_KEY, waitTime = LOCK_WAIT_SECONDS)
	@Transactional
	public void updateScorer(Long questionId, Long userId, Long submitOrder) {
		questionScorerEntityRepository.findByQuestionId(questionId).ifPresentOrElse(scorer -> {
			if (scorer.getSubmitOrder() > submitOrder) {
				scorer.updateScorer(userId, submitOrder);
				questionScorerEntityRepository.save(scorer);
			}
		}, () -> questionScorerEntityRepository.save(QuestionScorerEntity.builder()
			.questionId(questionId)
			.userId(userId)
			.submitOrder(submitOrder)
			.build()));
	}

	@DistributedLock(key = LOCK_KEY, waitTime = LOCK_WAIT_SECONDS)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recalculateScorer(Long questionId) {
		answerSubmitRecordEntityRepository
			.findFirstByQuestionIdAndIsCorrectTrueOrderBySubmitOrderAsc(questionId)
			.ifPresentOrElse(
				record -> applyScorer(questionId, record.getUserId(), record.getSubmitOrder()),
				() -> removeScorer(questionId));
	}

	private void applyScorer(Long questionId, Long userId, Long submitOrder) {
		questionScorerEntityRepository.findByQuestionId(questionId).ifPresentOrElse(scorer -> {
			scorer.updateScorer(userId, submitOrder);
			questionScorerEntityRepository.save(scorer);
		}, () -> questionScorerEntityRepository.save(QuestionScorerEntity.builder()
			.questionId(questionId)
			.userId(userId)
			.submitOrder(submitOrder)
			.build()));

		scorerProcessor.setScorer(questionId, userId, submitOrder);
	}

	private void removeScorer(Long questionId) {
		questionScorerEntityRepository.findByQuestionId(questionId)
			.ifPresent(questionScorerEntityRepository::delete);

		scorerProcessor.clearScorer(questionId);
	}
}
