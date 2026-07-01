package com.coniv.mait.web.admin.dto;

import com.coniv.mait.domain.team.enums.TeamUserRole;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OnboardingScreenUploadApiRequest(
	@Schema(description = "온보딩 화면 식별 코드", requiredMode = Schema.RequiredMode.REQUIRED, example = "QUESTION_SOLVE")
	@NotBlank(message = "온보딩 화면 코드는 필수입니다.")
	@Size(max = 64, message = "온보딩 화면 코드는 64자 이하여야 합니다.")
	@Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "온보딩 화면 코드는 대문자 영문, 숫자, 언더스코어만 사용할 수 있습니다.")
	String code,

	@Schema(description = "어드민 표시용 화면 이름", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "온보딩 화면 이름은 필수입니다.")
	String title,

	@Schema(description = "노출 대상 팀 권한 (null 이면 전체 유저 대상)", enumAsRef = true)
	TeamUserRole targetTeamRole
) {
}
