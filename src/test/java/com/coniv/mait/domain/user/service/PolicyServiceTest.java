package com.coniv.mait.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.entity.UserPolicyCheckHistory;
import com.coniv.mait.domain.user.enums.PolicyCategory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.enums.PolicyType;
import com.coniv.mait.domain.user.repository.PolicyEntityRepository;
import com.coniv.mait.domain.user.repository.UserPolicyCheckHistoryRepository;
import com.coniv.mait.domain.user.service.dto.PolicyDto;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

	@Mock
	private PolicyEntityRepository policyRepository;

	@Mock
	private UserPolicyCheckHistoryRepository userPolicyCheckHistoryRepository;

	@InjectMocks
	private PolicyService policyService;

	@Test
	@DisplayName("타이밍별 최신 정책 목록 조회 - 성공")
	void findLatestPolicies_Success() {
		// given
		PolicyTiming timing = PolicyTiming.SIGN_UP;

		PolicyEntity policy1V1 = PolicyEntity.builder()
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.TERMS_OF_SERVICE)
			.timing(PolicyTiming.SIGN_UP)
			.title("서비스 이용약관")
			.code("TERMS_SERVICE")
			.version(1)
			.content("서비스 이용약관 내용...")
			.build();

		PolicyEntity policy1V2 = PolicyEntity.builder()
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.TERMS_OF_SERVICE)
			.timing(PolicyTiming.SIGN_UP)
			.title("서비스 이용약관")
			.code("TERMS_SERVICE")
			.version(2)
			.content("서비스 이용약관 내용 v2...")
			.build();

		PolicyEntity policy2V1 = PolicyEntity.builder()
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.PERSONAL_INFORMATION)
			.timing(PolicyTiming.SIGN_UP)
			.title("개인정보 처리방침")
			.code("PRIVACY_POLICY")
			.version(1)
			.content("개인정보 처리방침 내용...")
			.build();

		when(policyRepository.findAllByTiming(timing))
			.thenReturn(List.of(policy1V1, policy1V2, policy2V1));

		// when
		List<PolicyDto> result = policyService.findLatestPolicies(timing);

		// then
		assertThat(result).hasSize(2); // TERMS_SERVICE v2, PRIVACY_POLICY v1
		verify(policyRepository).findAllByTiming(timing);
	}

	@Test
	@DisplayName("타이밍별 최신 정책 목록 조회 - 정책이 없는 경우 빈 리스트 반환")
	void findLatestPolicies_EmptyList() {
		// given
		PolicyTiming timing = PolicyTiming.SIGN_UP;
		when(policyRepository.findAllByTiming(timing)).thenReturn(List.of());

		// when
		List<PolicyDto> result = policyService.findLatestPolicies(timing);

		// then
		assertThat(result).isEmpty();
		verify(policyRepository).findAllByTiming(timing);
	}

	@Test
	@DisplayName("미확인 정책 목록 조회 - 사용자가 확인하지 않은 정책만 반환")
	void findUnConfirmedPolicies_Success() {
		// given
		Long userId = 1L;
		PolicyTiming timing = PolicyTiming.SIGN_UP;

		PolicyEntity policy1 = mock(PolicyEntity.class);
		when(policy1.getId()).thenReturn(1L);
		when(policy1.getCode()).thenReturn("TERMS_SERVICE");
		when(policy1.getVersion()).thenReturn(1);
		when(policy1.getPolicyType()).thenReturn(PolicyType.ESSENTIAL);
		when(policy1.getCategory()).thenReturn(PolicyCategory.TERMS_OF_SERVICE);
		when(policy1.getTiming()).thenReturn(PolicyTiming.SIGN_UP);
		when(policy1.getTitle()).thenReturn("서비스 이용약관");
		when(policy1.getContent()).thenReturn("서비스 이용약관 내용...");

		PolicyEntity policy2 = PolicyEntity.builder()
			.policyType(PolicyType.ESSENTIAL)
			.category(PolicyCategory.PERSONAL_INFORMATION)
			.timing(PolicyTiming.SIGN_UP)
			.title("개인정보 처리방침")
			.code("PRIVACY_POLICY")
			.version(1)
			.content("개인정보 처리방침 내용...")
			.build();

		PolicyEntity policy3 = PolicyEntity.builder()
			.policyType(PolicyType.OPTIONAL)
			.category(PolicyCategory.MARKETING)
			.timing(PolicyTiming.SIGN_UP)
			.title("마케팅 수신 동의")
			.code("MARKETING_AGREE")
			.version(1)
			.content("마케팅 수신 동의 내용...")
			.build();

		when(policyRepository.findAllByTiming(timing))
			.thenReturn(List.of(policy1, policy2, policy3));

		UserPolicyCheckHistory mockHistory = mock(UserPolicyCheckHistory.class);
		when(mockHistory.getPolicy()).thenReturn(policy1);

		when(userPolicyCheckHistoryRepository.findAllByUserId(userId))
			.thenReturn(List.of(mockHistory));

		// when
		List<PolicyDto> result = policyService.findUnConfirmedPolicies(userId, timing);

		// then
		assertThat(result).hasSize(2); // policy2, policy3
		verify(policyRepository).findAllByTiming(timing);
		verify(userPolicyCheckHistoryRepository).findAllByUserId(userId);
	}

	@Test
	@DisplayName("미확인 정책 목록 조회 - 모든 정책을 확인한 경우 빈 리스트 반환")
	void findUnConfirmedPolicies_AllConfirmed() {
		// given
		Long userId = 1L;
		PolicyTiming timing = PolicyTiming.SIGN_UP;

		PolicyEntity policy1 = mock(PolicyEntity.class);
		when(policy1.getId()).thenReturn(1L);
		when(policy1.getCode()).thenReturn("TERMS_SERVICE");
		when(policy1.getVersion()).thenReturn(1);
		when(policy1.getPolicyType()).thenReturn(PolicyType.ESSENTIAL);
		when(policy1.getCategory()).thenReturn(PolicyCategory.TERMS_OF_SERVICE);
		when(policy1.getTiming()).thenReturn(PolicyTiming.SIGN_UP);
		when(policy1.getTitle()).thenReturn("서비스 이용약관");
		when(policy1.getContent()).thenReturn("서비스 이용약관 내용...");

		when(policyRepository.findAllByTiming(timing))
			.thenReturn(List.of(policy1));

		UserPolicyCheckHistory mockHistory = mock(UserPolicyCheckHistory.class);
		when(mockHistory.getPolicy()).thenReturn(policy1);

		when(userPolicyCheckHistoryRepository.findAllByUserId(userId))
			.thenReturn(List.of(mockHistory));

		// when
		List<PolicyDto> result = policyService.findUnConfirmedPolicies(userId, timing);

		// then
		assertThat(result).isEmpty();
		verify(policyRepository).findAllByTiming(timing);
		verify(userPolicyCheckHistoryRepository).findAllByUserId(userId);
	}
}

