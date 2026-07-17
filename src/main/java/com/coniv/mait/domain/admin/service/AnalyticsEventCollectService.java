package com.coniv.mait.domain.admin.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;
import com.coniv.mait.domain.admin.entity.AnalyticsFeatureEntity;
import com.coniv.mait.domain.admin.repository.AnalyticsEventRepository;
import com.coniv.mait.domain.admin.repository.AnalyticsFeatureRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsEventCollectService {

	private final AnalyticsEventRepository analyticsEventRepository;
	private final AnalyticsFeatureRepository analyticsFeatureRepository;

	/**
	 * 분석 이벤트를 1건 저장한다.
	 *
	 * <p>{@code feature_key}는 미리 등록된 feature 마스터를 참조한다. 등록되지 않은 feature_key면 {@link EntityNotFoundException}을 던진다.
	 *
	 * <p>{@code (feature_id, event_name, user_id, session_id)} 조합이 이미 존재하면 저장하지 않는다(keep-first).
	 * 재전송/중복 전송을 멱등하게 무시하며, DB의 UNIQUE 제약이 동시성 backstop 역할을 한다.
	 */
	@Transactional
	public void collect(final Long userId, final String featureKey, final String eventName,
		final String sessionId, final Integer step, final String metadata) {
		AnalyticsFeatureEntity feature = analyticsFeatureRepository.findByFeatureKey(featureKey)
			.orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기능(feature)입니다: " + featureKey));

		boolean exists = analyticsEventRepository.existsByFeatureAndEventNameAndUserIdAndSessionId(
			feature, eventName, userId, sessionId);
		if (exists) {
			return;
		}

		analyticsEventRepository.save(AnalyticsEventEntity.builder()
			.feature(feature)
			.eventName(eventName)
			.userId(userId)
			.sessionId(sessionId)
			.step(step == null ? 0 : step)
			.occurredAt(LocalDateTime.now())
			.metadata(metadata)
			.build());
	}
}
