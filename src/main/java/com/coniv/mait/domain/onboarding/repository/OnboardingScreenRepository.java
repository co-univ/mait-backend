package com.coniv.mait.domain.onboarding.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;

public interface OnboardingScreenRepository extends JpaRepository<OnboardingScreenEntity, Long> {

	List<OnboardingScreenEntity> findAllByExposedTrue();
}
