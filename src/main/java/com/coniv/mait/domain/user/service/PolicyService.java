package com.coniv.mait.domain.user.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.entity.UserPolicyCheckHistory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.enums.PolicyType;
import com.coniv.mait.domain.user.repository.PolicyEntityRepository;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.repository.UserPolicyCheckHistoryRepository;
import com.coniv.mait.domain.user.service.dto.PolicyDto;
import com.coniv.mait.global.exception.custom.PolicyException;
import com.coniv.mait.web.user.dto.PolicyCheckRequest;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyService {

	private final PolicyEntityRepository policyRepository;
	private final UserPolicyCheckHistoryRepository userPolicyCheckHistoryRepository;
	private final UserEntityRepository userRepository;

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

	@Transactional
	public LocalDateTime checkPolicies(Long userId, List<PolicyCheckRequest> policyChecks) {
		UserEntity user = userRepository.findById(userId)
			.orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

		List<Long> policyIds = policyChecks.stream()
			.map(PolicyCheckRequest::policyId)
			.toList();

		List<PolicyEntity> policies = policyRepository.findAllById(policyIds);

		if (policies.size() != policyIds.size()) {
			throw new EntityNotFoundException("Some policies not found");
		}

		Map<Long, Boolean> checkMap = policyChecks.stream()
			.collect(Collectors.toMap(
				PolicyCheckRequest::policyId,
				PolicyCheckRequest::isChecked
			));

		validateEssentialPolicies(policies, checkMap);

		LocalDateTime checkTime = LocalDateTime.now();
		List<UserPolicyCheckHistory> histories = policies.stream()
			.map(policy -> {
				Boolean isChecked = checkMap.get(policy.getId());
				return UserPolicyCheckHistory.of(isChecked, user, policy, checkTime);
			})
			.toList();

		userPolicyCheckHistoryRepository.saveAll(histories);

		return checkTime;
	}

	private void validateEssentialPolicies(List<PolicyEntity> policies, Map<Long, Boolean> checkMap) {
		List<PolicyEntity> rejectedEssentialPolicies = policies.stream()
			.filter(policy -> policy.getPolicyType() == PolicyType.ESSENTIAL)
			.filter(policy -> !checkMap.get(policy.getId()))
			.toList();

		if (!rejectedEssentialPolicies.isEmpty()) {
			String rejectedPolicyTitles = rejectedEssentialPolicies.stream()
				.map(PolicyEntity::getTitle)
				.collect(Collectors.joining(", "));
			throw new PolicyException(
				"필수 정책에 동의하지 않았습니다: " + rejectedPolicyTitles
			);
		}
	}
}
