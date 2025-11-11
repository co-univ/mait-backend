package com.coniv.mait.web.user.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.user.enums.PolicyCategory;
import com.coniv.mait.domain.user.enums.PolicyTiming;
import com.coniv.mait.domain.user.enums.PolicyType;
import com.coniv.mait.domain.user.service.PolicyService;
import com.coniv.mait.domain.user.service.dto.PolicyDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyControllerTest {

	@MockitoBean
	private PolicyService policyService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);
	}

	@Test
	@DisplayName("타이밍별 최신 정책 목록 조회 API 테스트")
	void findLatestPolicies() throws Exception {
		// given
		PolicyTiming timing = PolicyTiming.SIGN_UP;

		List<PolicyDto> mockPolicies = List.of(
			PolicyDto.builder()
				.policyId(1L)
				.title("서비스 이용약관")
				.content("서비스 이용약관 내용...")
				.version(1)
				.policyType(PolicyType.ESSENTIAL)
				.timing(PolicyTiming.SIGN_UP)
				.category(PolicyCategory.TERMS_OF_SERVICE)
				.build(),
			PolicyDto.builder()
				.policyId(2L)
				.title("개인정보 처리방침")
				.content("개인정보 처리방침 내용...")
				.version(1)
				.policyType(PolicyType.ESSENTIAL)
				.timing(PolicyTiming.SIGN_UP)
				.category(PolicyCategory.PERSONAL_INFORMATION)
				.build()
		);

		when(policyService.findLatestPolicies(timing)).thenReturn(mockPolicies);

		// when & then
		mockMvc.perform(get("/api/v1/policies")
				.param("timing", timing.name())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].title").value("서비스 이용약관"))
			.andExpect(jsonPath("$.data[0].policyType").value("ESSENTIAL"))
			.andExpect(jsonPath("$.data[1].title").value("개인정보 처리방침"));

		verify(policyService, times(1)).findLatestPolicies(timing);
	}

	@Test
	@DisplayName("미확인 정책 목록 조회 API 테스트")
	void findUnConfirmedPolicies() throws Exception {
		// given
		Long userId = 1L;
		PolicyTiming timing = PolicyTiming.SIGN_UP;

		List<PolicyDto> mockPolicies = List.of(
			PolicyDto.builder()
				.policyId(2L)
				.title("개인정보 처리방침")
				.content("개인정보 처리방침 내용...")
				.version(1)
				.policyType(PolicyType.ESSENTIAL)
				.timing(PolicyTiming.SIGN_UP)
				.category(PolicyCategory.PERSONAL_INFORMATION)
				.build(),
			PolicyDto.builder()
				.policyId(3L)
				.title("마케팅 수신 동의")
				.content("마케팅 수신 동의 내용...")
				.version(1)
				.policyType(PolicyType.OPTIONAL)
				.timing(PolicyTiming.SIGN_UP)
				.category(PolicyCategory.MARKETING)
				.build()
		);

		when(policyService.findUnConfirmedPolicies(userId, timing)).thenReturn(mockPolicies);

		// when & then
		mockMvc.perform(get("/api/v1/policies/unchecked/" + userId)
				.param("timing", timing.name())
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data").isArray())
			.andExpect(jsonPath("$.data.length()").value(2))
			.andExpect(jsonPath("$.data[0].title").value("개인정보 처리방침"))
			.andExpect(jsonPath("$.data[1].title").value("마케팅 수신 동의"));

		verify(policyService, times(1)).findUnConfirmedPolicies(userId, timing);
	}
}

