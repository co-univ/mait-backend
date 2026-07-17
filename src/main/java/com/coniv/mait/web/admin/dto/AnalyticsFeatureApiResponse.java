package com.coniv.mait.web.admin.dto;

import com.coniv.mait.domain.admin.entity.AnalyticsFeatureEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record AnalyticsFeatureApiResponse(
	@Schema(description = "feature PK", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,

	@Schema(description = "feature 식별 키", example = "onboarding", requiredMode = Schema.RequiredMode.REQUIRED)
	String featureKey
) {
	public static AnalyticsFeatureApiResponse from(final AnalyticsFeatureEntity feature) {
		return AnalyticsFeatureApiResponse.builder()
			.id(feature.getId())
			.featureKey(feature.getFeatureKey())
			.build();
	}
}
