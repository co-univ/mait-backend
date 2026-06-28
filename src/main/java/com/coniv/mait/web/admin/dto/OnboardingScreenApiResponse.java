package com.coniv.mait.web.admin.dto;

import java.time.LocalDateTime;

import com.coniv.mait.domain.onboarding.enums.OnboardingScreenCode;
import com.coniv.mait.domain.onboarding.service.dto.OnboardingScreenDto;
import com.coniv.mait.domain.team.enums.TeamUserRole;

import io.swagger.v3.oas.annotations.media.Schema;

public record OnboardingScreenApiResponse(
	@Schema(description = "온보딩 화면 PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,

	@Schema(description = "온보딩 화면 식별 코드", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	OnboardingScreenCode code,

	@Schema(description = "어드민 표시용 화면 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	String title,

	@Schema(description = "전역 노출 여부", requiredMode = Schema.RequiredMode.REQUIRED)
	boolean exposed,

	@Schema(description = "노출 대상 팀 권한 (null 이면 전체 유저 대상)", enumAsRef = true)
	TeamUserRole targetTeamRole,

	@Schema(description = "생성 일시", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDateTime createdAt,

	@Schema(description = "수정 일시", requiredMode = Schema.RequiredMode.REQUIRED)
	LocalDateTime modifiedAt
) {
	public static OnboardingScreenApiResponse from(final OnboardingScreenDto screen) {
		return new OnboardingScreenApiResponse(
			screen.getId(),
			screen.getCode(),
			screen.getTitle(),
			screen.isExposed(),
			screen.getTargetTeamRole(),
			screen.getCreatedAt(),
			screen.getModifiedAt());
	}
}
