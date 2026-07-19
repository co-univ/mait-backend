package com.coniv.mait.domain.admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.admin.entity.AnalyticsFeatureEntity;

public interface AnalyticsFeatureRepository extends JpaRepository<AnalyticsFeatureEntity, Long> {

	Optional<AnalyticsFeatureEntity> findByFeatureKey(String featureKey);
}
