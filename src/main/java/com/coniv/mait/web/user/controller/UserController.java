package com.coniv.mait.web.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.service.UserService;
import com.coniv.mait.domain.user.service.dto.UserDto;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.user.dto.UserInfoApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@Operation(summary = "사용자 정보 반환")
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserInfoApiResponse>> getUserInfo(@AuthenticationPrincipal UserEntity user) {
		UserDto dto = userService.getUserInfo(user);
		return ResponseEntity.ok(ApiResponse.ok(UserInfoApiResponse.from(dto)));
	}
}
