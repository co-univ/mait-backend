package com.coniv.mait.web.user.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.user.service.UserService;
import com.coniv.mait.domain.user.service.dto.UserDto;
import com.coniv.mait.global.auth.cookie.CookieFactory;
import com.coniv.mait.global.auth.model.MaitUser;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.user.dto.UpdateNicknameRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

	private static final Long USER_ID = 1L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private IdempotencyInterceptor idempotencyInterceptor;

	@MockitoBean
	private JwtAuthorizationFilter jwtAuthorizationFilter;

	@MockitoBean
	private CookieFactory cookieFactory;

	@BeforeEach
	void setUp() throws Exception {
		when(idempotencyInterceptor.preHandle(any(), any(), any())).thenReturn(true);

		MaitUser user = MaitUser.builder().id(USER_ID).build();
		var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
		var context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@DisplayName("사용자 정보 반환 API 테스트")
	void getUserInfo() throws Exception {
		// given
		UserDto mockDto = UserDto.builder()
			.id(1L)
			.email("test@example.com")
			.name("테스트유저")
			.nickname("빠른고양이")
			.nicknameCode("1234")
			.fullNickname("빠른고양이#1234")
			.build();

		when(userService.getUserInfo(USER_ID)).thenReturn(mockDto);

		// when & then
		mockMvc.perform(get("/api/v1/users/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.email").value("test@example.com"))
			.andExpect(jsonPath("$.data.name").value("테스트유저"))
			.andExpect(jsonPath("$.data.nickname").value("빠른고양이"))
			.andExpect(jsonPath("$.data.fullNickname").value("빠른고양이#1234"));

		verify(userService).getUserInfo(USER_ID);
	}

	@Test
	@DisplayName("닉네임 변경 API 테스트 - 성공")
	void updateUserNickname_Success() throws Exception {
		// given
		UpdateNicknameRequest request = new UpdateNicknameRequest("새로운닉네임");
		UserDto mockDto = UserDto.builder()
			.id(1L)
			.email("test@example.com")
			.name("테스트유저")
			.nickname("새로운닉네임")
			.nicknameCode("5678")
			.fullNickname("새로운닉네임#5678")
			.build();

		when(userService.updateUserNickname(eq(USER_ID), eq("새로운닉네임"))).thenReturn(mockDto);

		// when & then
		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.nickname").value("새로운닉네임"))
			.andExpect(jsonPath("$.data.fullNickname").value("새로운닉네임#5678"));

		verify(userService).updateUserNickname(eq(USER_ID), eq("새로운닉네임"));
	}

	@Test
	@DisplayName("닉네임 변경 API 테스트 - 실패: 닉네임이 null")
	void updateUserNickname_Failure_NullNickname() throws Exception {
		// given
		String requestBody = "{\"nickname\": null}";

		// when & then
		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest());

		verify(userService, never()).updateUserNickname(anyLong(), any());
	}

	@Test
	@DisplayName("닉네임 변경 API 테스트 - 실패: 닉네임이 너무 짧음 (trim 후)")
	void updateUserNickname_Failure_TooShortAfterTrim() throws Exception {
		// given
		UpdateNicknameRequest request = new UpdateNicknameRequest("  a  ");

		// when & then
		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest());

		verify(userService, never()).updateUserNickname(anyLong(), any());
	}

	@Test
	@DisplayName("랜덤 닉네임 반환 API 테스트")
	void getRandomNickname() throws Exception {
		// given
		String randomNickname = "귀여운토끼";
		when(userService.getRandomNickname()).thenReturn(randomNickname);

		// when & then
		mockMvc.perform(get("/api/v1/users/nickname/random"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.nickname").value(randomNickname));

		verify(userService).getRandomNickname();
	}
}
