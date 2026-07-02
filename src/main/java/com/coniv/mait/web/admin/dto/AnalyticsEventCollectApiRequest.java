package com.coniv.mait.web.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record AnalyticsEventCollectApiRequest(

	@Schema(description = "기능 그룹", requiredMode = Schema.RequiredMode.REQUIRED, example = "onboarding")
	@NotBlank(message = "기능 그룹은 필수입니다.")
	String featureKey,

	@Schema(description = "이벤트명", requiredMode = Schema.RequiredMode.REQUIRED, example = "player_set_list_view")
	@NotBlank(message = "이벤트명은 필수입니다.")
	String eventName,

	@Schema(description = "FE에서 생성한 세션 UUID", requiredMode = Schema.RequiredMode.REQUIRED,
		example = "550e8400-e29b-41d4-a716-446655440000")
	@NotBlank(message = "세션 ID는 필수입니다.")
	String sessionId,

	@Schema(description = "단계. 없으면 0", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "2")
	@PositiveOrZero(message = "단계는 0 이상이어야 합니다.")
	Integer step,

	@Schema(description = "추가 컨텍스트 JSON 문자열", requiredMode = Schema.RequiredMode.NOT_REQUIRED,
		example = "{\"screen\":\"solve-question-list\"}")
	String metadata
) {
}
