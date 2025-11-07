package com.coniv.mait.web.user.dto;

import com.coniv.mait.domain.user.enums.PolicyCategory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.enums.PolicyType;
import com.coniv.mait.domain.user.service.dto.PolicyDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LatestPoliciesApiResponse(

	@Schema(description = "정책 버전 ID", requiredMode = Schema.RequiredMode.REQUIRED)
	Long id,

	@Schema(description = "정책 제목", requiredMode = Schema.RequiredMode.REQUIRED)
	String title,

	@Schema(description = "정책 내용", requiredMode = Schema.RequiredMode.REQUIRED)
	String content,

	@Schema(description = "정책 동의 필수", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	PolicyType policyType,

	@Schema(description = "정책 적용 타이밍", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	PolicyTiming timing,

	@Schema(description = "정책 카테고리", requiredMode = Schema.RequiredMode.REQUIRED, enumAsRef = true)
	PolicyCategory category
) {

	public static LatestPoliciesApiResponse from(PolicyDto policyDto) {
		return new LatestPoliciesApiResponse(
			policyDto.getPolicyVersionId(),
			policyDto.getTitle(),
			policyDto.getContent(),
			policyDto.getPolicyType(),
			policyDto.getTiming(),
			policyDto.getCategory()
		);
	}
}
