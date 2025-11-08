package com.coniv.mait.domain.user.service.dto;

import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.enums.PolicyCategory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.enums.PolicyType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PolicyDto {

	private Long policyId;

	private String title;

	private String content;

	private Integer version;

	private PolicyType policyType;

	private PolicyTiming timing;

	private PolicyCategory category;

	public static PolicyDto from(PolicyEntity policy) {
		return new PolicyDto(
			policy.getId(),
			policy.getTitle(),
			policy.getContent(),
			policy.getVersion(),
			policy.getPolicyType(),
			policy.getTiming(),
			policy.getCategory()
		);
	}
}
