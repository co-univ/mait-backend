package com.coniv.mait.domain.question.event;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.coniv.mait.domain.question.service.QuestionImageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionSetDeletedEventListener {

	private static final String SUBMIT_ORDER_PREFIX = "$submit:order:questionId:";
	private static final String SCORER_PREFIX = "$scorer:questionId:";
	private static final String AI_STATUS_PREFIX = "question_set_ai_status:";
	private static final String REVIEW_PREFIX = "review:last_viewed:question_set:";

	private final RedisTemplate<String, String> redisTemplate;
	private final QuestionImageService questionImageService;

	@Async("maitThreadPoolExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleQuestionSetDeleted(final QuestionSetDeletedEvent event) {
		cleanupRedisKeys(event);
		cleanupImages(event.imageIds());
	}

	private void cleanupRedisKeys(QuestionSetDeletedEvent event) {
		try {
			List<String> keys = new ArrayList<>();

			for (Long questionId : event.questionIds()) {
				keys.add(SUBMIT_ORDER_PREFIX + questionId);
				keys.add(SCORER_PREFIX + questionId);
			}

			keys.add(AI_STATUS_PREFIX + event.questionSetId());

			String reviewPattern = REVIEW_PREFIX + event.questionSetId() + ":*";
			ScanOptions scanOptions = ScanOptions.scanOptions()
				.match(reviewPattern).count(100).build();
			try (var cursor = redisTemplate.scan(scanOptions)) {
				cursor.forEachRemaining(keys::add);
			}

			if (!keys.isEmpty()) {
				Long deleted = redisTemplate.delete(keys);
				log.info("[Redis 키 정리] questionSetId={}, 삭제 키 수={}", event.questionSetId(), deleted);
			}
		} catch (Exception e) {
			log.error("[Redis 키 정리 실패] questionSetId={}", event.questionSetId(), e);
		}
	}

	private void cleanupImages(List<Long> imageIds) {
		for (Long imageId : imageIds) {
			try {
				questionImageService.unUseExistImage(imageId);
			} catch (Exception e) {
				log.error("[이미지 미사용 처리 실패] imageId={}", imageId, e);
			}
		}
	}
}
