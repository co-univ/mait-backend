package com.coniv.mait.domain.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coniv.mait.domain.onboarding.entity.UserOnboardingViewEntity;
import com.coniv.mait.domain.onboarding.entity.UserOnboardingViewId;

public interface UserOnboardingViewRepository extends JpaRepository<UserOnboardingViewEntity, UserOnboardingViewId> {

	long countByOnboardingScreen_Id(Long onboardingScreenId);
}
