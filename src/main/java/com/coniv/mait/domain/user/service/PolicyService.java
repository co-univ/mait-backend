package com.coniv.mait.domain.user.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.repository.PolicyEntityRepository;
import com.coniv.mait.domain.user.service.dto.PolicyDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyService {

	private final PolicyEntityRepository policyRepository;

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

	//해당 유저가 동의하지 않은 정책 조회
}


