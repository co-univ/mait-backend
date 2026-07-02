package com.coniv.mait.domain.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.admin.entity.AnalyticsEventEntity;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEventEntity, Long> {
}
