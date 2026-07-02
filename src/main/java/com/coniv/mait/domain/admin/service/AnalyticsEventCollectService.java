package com.coniv.mait.domain.admin.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;
import com.coniv.mait.domain.admin.repository.AnalyticsEventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsEventCollectService {

	private final AnalyticsEventRepository analyticsEventRepository;

	/**
	 * 분석 이벤트를 1건 저장한다.
	 *
	 * <p>{@code (feature_key, event_name, user_id, session_id)} 조합이 이미 존재하면 저장하지 않는다(keep-first).
	 * 재전송/중복 전송을 멱등하게 무시하며, DB의 UNIQUE 제약이 동시성 backstop 역할을 한다.
	 */
	@Transactional
	public void collect(final Long userId, final String featureKey, final String eventName,
		final String sessionId, final Integer step, final String metadata) {
		boolean exists = analyticsEventRepository.existsByFeatureKeyAndEventNameAndUserIdAndSessionId(
			featureKey, eventName, userId, sessionId);
		if (exists) {
			return;
		}

		analyticsEventRepository.save(AnalyticsEventEntity.builder()
			.featureKey(featureKey)
			.eventName(eventName)
			.userId(userId)
			.sessionId(sessionId)
			.step(step == null ? 0 : step)
			.occurredAt(LocalDateTime.now())
			.metadata(metadata)
			.build());
	}
}
