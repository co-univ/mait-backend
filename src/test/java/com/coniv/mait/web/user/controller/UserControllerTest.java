package com.coniv.mait.web.user.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.service.UserService;
import com.coniv.mait.domain.user.service.dto.UserDto;
import com.coniv.mait.global.filter.JwtAuthorizationFilter;
import com.coniv.mait.global.interceptor.idempotency.IdempotencyInterceptor;
import com.coniv.mait.web.user.dto.UpdateNicknameRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

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

		when(userService.getUserInfo(nullable(UserEntity.class))).thenReturn(mockDto);

		// when & then
		mockMvc.perform(get("/api/v1/users/me"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.email").value("test@example.com"))
			.andExpect(jsonPath("$.data.name").value("테스트유저"))
			.andExpect(jsonPath("$.data.nickname").value("빠른고양이"))
			.andExpect(jsonPath("$.data.fullNickname").value("빠른고양이#1234"));

		verify(userService).getUserInfo(nullable(UserEntity.class));
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

		when(userService.updateUserNickname(nullable(UserEntity.class), eq("새로운닉네임"))).thenReturn(mockDto);

		// when & then
		mockMvc.perform(patch("/api/v1/users/nickname")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.isSuccess").value(true))
			.andExpect(jsonPath("$.data.nickname").value("새로운닉네임"))
			.andExpect(jsonPath("$.data.fullNickname").value("새로운닉네임#5678"));

		verify(userService).updateUserNickname(nullable(UserEntity.class), eq("새로운닉네임"));
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

		verify(userService, never()).updateUserNickname(nullable(UserEntity.class), any());
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

		verify(userService, never()).updateUserNickname(nullable(UserEntity.class), any());
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

