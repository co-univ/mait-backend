package com.coniv.mait.domain.solve.service.component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.solve.entity.QuestionScorerEntity;
import com.coniv.mait.domain.solve.repository.QuestionScorerEntityRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScorerGenerator {

	private static final String KEY_PREFIX = "$scorer:update:questionId:";

	private final QuestionScorerEntityRepository questionScorerEntityRepository;

	private final RedissonClient redissonClient;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateScorer(Long questionId, Long userId, Long submitOrder) {
		RLock lock = redissonClient.getLock(generateKey(questionId));
		try {
			boolean available = lock.tryLock(30, 1, TimeUnit.SECONDS);

			if (!available) {
				log.error("[락 획득 실패] questionId: {} , userId: {}", questionId, userId);
				throw new RuntimeException("Scorer lock error");
			}

			updateOrInsertScorer(questionId, userId, submitOrder);

		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			if (lock.isLocked() && lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}

	private void updateOrInsertScorer(Long questionId, Long userId, Long submitOrder) {
		Optional<QuestionScorerEntity> questionScorer = questionScorerEntityRepository.findByQuestionId(questionId);
		questionScorer.ifPresentOrElse(scorer -> {
			if (scorer.getSubmitOrder() > submitOrder) {
				scorer.updateScorer(userId, submitOrder);
				questionScorerEntityRepository.save(scorer);
			}
		}, () -> {
			QuestionScorerEntity newScorer = QuestionScorerEntity.builder()
				.questionId(questionId)
				.userId(userId)
				.submitOrder(submitOrder)
				.build();
			questionScorerEntityRepository.save(newScorer);
		});
	}

	private String generateKey(final Long questionId) {
		return KEY_PREFIX + questionId;
	}
}
