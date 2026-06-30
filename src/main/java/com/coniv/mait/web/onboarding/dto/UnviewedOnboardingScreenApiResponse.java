package com.coniv.mait.web.onboarding.dto;

import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record UnviewedOnboardingScreenApiResponse(

	@Schema(description = "온보딩 화면 ID", example = "1")
	Long id,

	@Schema(description = "노출 대상 팀 권한 (null 이면 전체 유저 대상)", example = "MAKER", enumAsRef = true)
	TeamUserRole targetTeamRole,

	@Schema(description = "온보딩 화면 코드", example = "QUESTION_SOLVE")
	String code,

	@Schema(description = "온보딩 화면 이름", example = "문제 풀기 가이드")
	String title
) {

	public static UnviewedOnboardingScreenApiResponse from(final OnboardingScreenDto screen) {
		return UnviewedOnboardingScreenApiResponse.builder()
			.id(screen.getId())
			.targetTeamRole(screen.getTargetTeamRole())
			.code(screen.getCode())
			.title(screen.getTitle())
			.build();
	}
}
