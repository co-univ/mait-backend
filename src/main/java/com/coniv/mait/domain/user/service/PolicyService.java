package com.coniv.mait.domain.user.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.entity.UserPolicyCheckHistory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.repository.PolicyEntityRepository;
import com.coniv.mait.domain.user.repository.UserPolicyCheckHistoryRepository;
import com.coniv.mait.domain.user.service.dto.PolicyDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyService {

	private final PolicyEntityRepository policyRepository;
	private final UserPolicyCheckHistoryRepository userPolicyCheckHistoryRepository;

	@Transactional(readOnly = true)
	public List<PolicyDto> findLatestPolicies(PolicyTiming policyTiming) {
		List<PolicyEntity> policies = policyRepository.findAllByTiming(policyTiming);
		if (policies.isEmpty()) {
			return List.of();
		}

		Map<String, List<PolicyEntity>> groupByCode = policies.stream()
			.collect(Collectors.groupingBy(PolicyEntity::getCode));

		return groupByCode.values().stream()
			.map(list -> list.stream()
				.max(Comparator.comparing(PolicyEntity::getVersion))
				.orElse(null)
			)
			.filter(Objects::nonNull)
			.map(PolicyDto::from)
			.toList();
	}

	//해당 유저가 약관을 확인하지 않은 항목 반환
	@Transactional(readOnly = true)
	public List<PolicyDto> findUnConfirmedPolicies(Long userId, PolicyTiming policyTiming) {
		List<PolicyDto> latestPolicies = findLatestPolicies(policyTiming);

		if (latestPolicies.isEmpty()) {
			return List.of();
		}

		Set<Long> confirmedPolicyIds = userPolicyCheckHistoryRepository.findAllByUserId(userId).stream()
			.map(UserPolicyCheckHistory::getPolicy)
			.map(PolicyEntity::getId)
			.collect(Collectors.toSet());

		return latestPolicies.stream()
			.filter(policy -> !confirmedPolicyIds.contains(policy.getPolicyId()))
			.toList();
	}
}
