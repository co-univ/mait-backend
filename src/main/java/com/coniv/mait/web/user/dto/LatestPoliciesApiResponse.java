package com.coniv.mait.web.user.dto;

import com.coniv.mait.domain.user.enums.PolicyCategory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.enums.PolicyType;
import com.coniv.mait.domain.user.service.dto.PolicyDto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LatestPoliciesApiResponse(

	@Schema(description = "정책 버전 ID")
	Long id,

	@Schema(description = "정책 제목")
	String title,

	@Schema(description = "정책 내용")
	String content,

	@Schema(description = "정책 동의 필수")
	PolicyType policyType,

	@Schema(description = "정책 적용 타이밍")
	PolicyTiming timing,

	@Schema(description = "정책 카테고리")
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
