package com.coniv.mait.domain.user.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.entity.PolicyVersionEntity;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.repository.PolicyEntityRepository;
import com.coniv.mait.domain.user.repository.PolicyVersionEntityRepository;
import com.coniv.mait.domain.user.service.dto.PolicyDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyService {

	private final PolicyEntityRepository policyRepository;
	private final PolicyVersionEntityRepository policyVersionRepository;

	@Transactional(readOnly = true)
	public List<PolicyDto> findLatestPolicies(PolicyTiming policyTiming) {
		List<PolicyEntity> policies = policyRepository.findAllByTiming(policyTiming);
		if (policies.isEmpty()) {
			return List.of();
		}
		List<PolicyVersionEntity> latestVersions = policyVersionRepository.findLatestVersionsByPolicies(policies);

		Map<Long, PolicyVersionEntity> latestByPolicyId = latestVersions.stream()
			.collect(Collectors.toMap(pv -> pv.getPolicy().getId(), Function.identity()));

		return policies.stream()
			.map(p -> latestByPolicyId.get(p.getId()))
			.filter(Objects::nonNull)
			.map(pv -> PolicyDto.from(pv.getPolicy(), pv))
			.toList();
	}

	//해당 유저가 동의하지 않은 정책 조회
}


