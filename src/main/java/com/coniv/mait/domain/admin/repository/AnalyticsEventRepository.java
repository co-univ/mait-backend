package com.coniv.mait.domain.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;
import com.coniv.mait.domain.admin.entity.AnalyticsFeatureEntity;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEventEntity, Long> {

	boolean existsByFeatureAndEventNameAndUserIdAndSessionId(
		AnalyticsFeatureEntity feature, String eventName, Long userId, String sessionId);

	List<AnalyticsEventEntity> findAllByFeature(AnalyticsFeatureEntity feature);
}
