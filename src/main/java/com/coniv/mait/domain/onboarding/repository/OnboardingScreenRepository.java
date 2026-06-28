package com.coniv.mait.domain.onboarding.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.enums.OnboardingScreenCode;

public interface OnboardingScreenRepository extends JpaRepository<OnboardingScreenEntity, Long> {

	Optional<OnboardingScreenEntity> findByCode(OnboardingScreenCode code);

	boolean existsByCode(OnboardingScreenCode code);
}
