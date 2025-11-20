package com.coniv.mait.domain.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.coniv.mait.domain.user.entity.PolicyEntity;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.entity.UserPolicyCheckHistory;
import com.coniv.mait.domain.user.enums.PolicyCategory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.enums.PolicyType;
import com.coniv.mait.domain.user.repository.PolicyEntityRepository;
import com.coniv.mait.domain.user.repository.UserEntityRepository;
import com.coniv.mait.domain.user.repository.UserPolicyCheckHistoryRepository;
import com.coniv.mait.domain.user.service.component.PolicyReader;
import com.coniv.mait.domain.user.service.dto.PolicyDto;
import com.coniv.mait.global.exception.custom.PolicyException;
import com.coniv.mait.web.user.dto.PolicyCheckRequest;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

	@Mock
	private PolicyEntityRepository policyRepository;

	@Mock
	private UserPolicyCheckHistoryRepository userPolicyCheckHistoryRepository;

	@Mock
	private UserEntityRepository userRepository;

	@Mock
	private PolicyReader policyReader;

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

		when(policyReader.findLatestPolicies(timing))
			.thenReturn(List.of(PolicyDto.from(policy1V2), PolicyDto.from(policy2V1)));

		// when
		List<PolicyDto> result = policyService.findLatestPolicies(timing);

		// then
		assertThat(result).hasSize(2); // TERMS_SERVICE v2, PRIVACY_POLICY v1
		verify(policyReader).findLatestPolicies(timing);
	}

	@Test
	@DisplayName("타이밍별 최신 정책 목록 조회 - 정책이 없는 경우 빈 리스트 반환")
	void findLatestPolicies_EmptyList() {
		// given
		PolicyTiming timing = PolicyTiming.SIGN_UP;
		when(policyReader.findLatestPolicies(timing)).thenReturn(List.of());

		// when
		List<PolicyDto> result = policyService.findLatestPolicies(timing);

		// then
		assertThat(result).isEmpty();
		verify(policyReader).findLatestPolicies(timing);
	}

	@Test
	@DisplayName("미확인 정책 목록 조회 - 사용자가 확인하지 않은 정책만 반환")
	void findUnConfirmedPolicies_Success() {
		// given
		Long userId = 1L;
		PolicyTiming timing = PolicyTiming.SIGN_UP;

		PolicyEntity policy1 = mock(PolicyEntity.class);
		when(policy1.getId()).thenReturn(1L);

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

		when(policyReader.findLatestPolicies(timing))
			.thenReturn(List.of(
				PolicyDto.builder()
					.policyId(1L)
					.title("서비스 이용약관")
					.content("서비스 이용약관 내용...")
					.version(1)
					.policyType(PolicyType.ESSENTIAL)
					.timing(PolicyTiming.SIGN_UP)
					.category(PolicyCategory.TERMS_OF_SERVICE)
					.build(),
				PolicyDto.from(policy2),
				PolicyDto.from(policy3)
			));

		UserPolicyCheckHistory mockHistory = mock(UserPolicyCheckHistory.class);
		when(mockHistory.getPolicy()).thenReturn(policy1);

		when(userPolicyCheckHistoryRepository.findAllByUserId(userId))
			.thenReturn(List.of(mockHistory));

		// when
		List<PolicyDto> result = policyService.findUnConfirmedPolicies(userId, timing);

		// then
		assertThat(result).hasSize(2); // policy2, policy3
		verify(policyReader).findLatestPolicies(timing);
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

		when(policyReader.findLatestPolicies(timing))
			.thenReturn(List.of(
				PolicyDto.builder()
					.policyId(1L)
					.title("서비스 이용약관")
					.content("서비스 이용약관 내용...")
					.version(1)
					.policyType(PolicyType.ESSENTIAL)
					.timing(PolicyTiming.SIGN_UP)
					.category(PolicyCategory.TERMS_OF_SERVICE)
					.build()
			));

		UserPolicyCheckHistory mockHistory = mock(UserPolicyCheckHistory.class);
		when(mockHistory.getPolicy()).thenReturn(policy1);

		when(userPolicyCheckHistoryRepository.findAllByUserId(userId))
			.thenReturn(List.of(mockHistory));

		// when
		List<PolicyDto> result = policyService.findUnConfirmedPolicies(userId, timing);

		// then
		assertThat(result).isEmpty();
		verify(policyReader).findLatestPolicies(timing);
		verify(userPolicyCheckHistoryRepository).findAllByUserId(userId);
	}

	@Test
	@DisplayName("정책 체크 - 성공")
	void checkPolicies_Success() {
		// given
		Long userId = 1L;
		UserEntity mockUser = mock(UserEntity.class);

		PolicyEntity policy1 = mock(PolicyEntity.class);
		when(policy1.getId()).thenReturn(1L);
		when(policy1.getPolicyType()).thenReturn(PolicyType.ESSENTIAL);

		PolicyEntity policy2 = mock(PolicyEntity.class);
		when(policy2.getId()).thenReturn(2L);
		when(policy2.getPolicyType()).thenReturn(PolicyType.OPTIONAL);

		List<PolicyCheckRequest> policyChecks = List.of(
			new PolicyCheckRequest(1L, true),
			new PolicyCheckRequest(2L, false)
		);

		when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));
		when(policyRepository.findAllById(anyList())).thenReturn(List.of(policy1, policy2));

		// when
		LocalDateTime result = policyService.checkPolicies(userId, policyChecks);

		// then
		assertThat(result).isNotNull();
		verify(userRepository).findById(userId);
		verify(policyRepository).findAllById(anyList());
		verify(userPolicyCheckHistoryRepository).saveAll(anyList());
	}

	@Test
	@DisplayName("정책 체크 - 필수 정책 거부 시 예외 발생")
	void checkPolicies_RejectEssential_ThrowsException() {
		// given
		Long userId = 1L;
		UserEntity mockUser = mock(UserEntity.class);

		PolicyEntity policy1 = mock(PolicyEntity.class);
		when(policy1.getId()).thenReturn(1L);
		when(policy1.getPolicyType()).thenReturn(PolicyType.ESSENTIAL);
		when(policy1.getTitle()).thenReturn("서비스 이용약관");

		List<PolicyCheckRequest> policyChecks = List.of(
			new PolicyCheckRequest(1L, false) // 필수 정책 거부
		);

		when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(mockUser));
		when(policyRepository.findAllById(anyList())).thenReturn(List.of(policy1));

		// when & then
		assertThatThrownBy(() -> policyService.checkPolicies(userId, policyChecks))
			.isInstanceOf(PolicyException.class)
			.hasMessageContaining("필수 정책에 동의하지 않았습니다");

		verify(userPolicyCheckHistoryRepository, never()).saveAll(anyList());
	}

	@Test
	@DisplayName("정책 체크 - 존재하지 않는 사용자")
	void checkPolicies_UserNotFound() {
		// given
		Long userId = 999L;
		List<PolicyCheckRequest> policyChecks = List.of(
			new PolicyCheckRequest(1L, true)
		);

		when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

		// when & then
		assertThatThrownBy(() -> policyService.checkPolicies(userId, policyChecks))
			.isInstanceOf(EntityNotFoundException.class)
			.hasMessageContaining("User not found");

		verify(policyRepository, never()).findAllById(anyList());
	}
}
