package com.coniv.mait.domain.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;
import com.coniv.mait.domain.admin.entity.AnalyticsFeatureEntity;
import com.coniv.mait.domain.admin.repository.AnalyticsEventRepository;
import com.coniv.mait.domain.admin.repository.AnalyticsFeatureRepository;
import com.coniv.mait.domain.admin.service.dto.AnalyticsEventStatsDto;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsEventQueryService {

	private final AnalyticsFeatureRepository analyticsFeatureRepository;
	private final AnalyticsEventRepository analyticsEventRepository;

	public List<AnalyticsFeatureEntity> getFeatures() {
		return analyticsFeatureRepository.findAll();
	}

	public AnalyticsEventStatsDto getEventStats(final Long featureId) {
		AnalyticsFeatureEntity feature = analyticsFeatureRepository.findById(featureId)
			.orElseThrow(() -> new EntityNotFoundException("등록되지 않은 기능(feature)입니다: " + featureId));

		List<AnalyticsEventEntity> events = analyticsEventRepository.findAllByFeature(feature);
		return AnalyticsEventStatsDto.of(feature.getFeatureKey(), events);
	}
}
