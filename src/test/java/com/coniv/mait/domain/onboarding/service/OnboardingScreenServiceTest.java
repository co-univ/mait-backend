package com.coniv.mait.domain.onboarding.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.onboarding.entity.OnboardingScreenEntity;
import com.coniv.mait.domain.onboarding.repository.OnboardingScreenRepository;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;

@ExtendWith(MockitoExtension.class)
class OnboardingScreenServiceTest {

	@Mock
	private OnboardingScreenRepository onboardingScreenRepository;

	@InjectMocks
	private OnboardingScreenService onboardingScreenService;

	@Test
	@DisplayName("온보딩 화면을 기본 노출(true) 상태로 새로 등록한다")
	void uploadScreen_create() {
		// given
		String code = "QUESTION_SOLVE";
		given(onboardingScreenRepository.findByCode(code)).willReturn(Optional.empty());
		given(onboardingScreenRepository.save(any(OnboardingScreenEntity.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		OnboardingScreenDto result = onboardingScreenService.uploadScreen(code, "문제 풀기 가이드", TeamUserRole.MAKER);

		// then
		ArgumentCaptor<OnboardingScreenEntity> captor = ArgumentCaptor.forClass(OnboardingScreenEntity.class);
		then(onboardingScreenRepository).should().save(captor.capture());
		OnboardingScreenEntity saved = captor.getValue();
		assertThat(saved.getCode()).isEqualTo(code);
		assertThat(saved.getTitle()).isEqualTo("문제 풀기 가이드");
		assertThat(saved.isExposed()).isTrue();
		assertThat(saved.getTargetTeamRole()).isEqualTo(TeamUserRole.MAKER);

		assertThat(result.getCode()).isEqualTo(code);
		assertThat(result.getTitle()).isEqualTo("문제 풀기 가이드");
		assertThat(result.isExposed()).isTrue();
		assertThat(result.getTargetTeamRole()).isEqualTo(TeamUserRole.MAKER);
	}

	@Test
	@DisplayName("동일 code 의 온보딩 화면이 있으면 새로 만들지 않고 갱신한다")
	void uploadScreen_updateExisting() {
		// given
		String code = "QUESTION_SOLVE";
		OnboardingScreenEntity existingScreen = OnboardingScreenEntity.builder()
			.code(code)
			.title("기존 가이드")
			.exposed(true)
			.targetTeamRole(TeamUserRole.PLAYER)
			.build();
		given(onboardingScreenRepository.findByCode(code)).willReturn(Optional.of(existingScreen));

		// when
		OnboardingScreenDto result = onboardingScreenService.uploadScreen(code, "문제 풀기 가이드", TeamUserRole.MAKER);

		// then
		then(onboardingScreenRepository).should(never()).save(any());
		assertThat(existingScreen.getTitle()).isEqualTo("문제 풀기 가이드");
		assertThat(existingScreen.getTargetTeamRole()).isEqualTo(TeamUserRole.MAKER);
		assertThat(result.getCode()).isEqualTo(code);
		assertThat(result.getTitle()).isEqualTo("문제 풀기 가이드");
		assertThat(result.getTargetTeamRole()).isEqualTo(TeamUserRole.MAKER);
	}
}
