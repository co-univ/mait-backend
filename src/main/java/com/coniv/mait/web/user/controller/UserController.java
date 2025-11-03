package com.coniv.mait.web.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.service.UserService;
import com.coniv.mait.domain.user.service.dto.UserDto;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.user.dto.PatchNicknameRequest;
import com.coniv.mait.web.user.dto.PatchNicknameResponse;
import com.coniv.mait.web.user.dto.RandomNicknameResponse;
import com.coniv.mait.web.user.dto.UserInfoApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
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

	@Operation(summary = "사용자 닉네임 변경")
	@PatchMapping("/nickname")
	public ResponseEntity<ApiResponse<PatchNicknameResponse>> updateUserNickname(
		@AuthenticationPrincipal UserEntity user,
		@Valid @RequestBody PatchNicknameRequest request) {
		UserDto dto = userService.updateUserNickname(user, request.nickname().trim());
		return ResponseEntity.ok(ApiResponse.ok(PatchNicknameResponse.from(dto)));
	}

	@Operation(summary = "랜덤 닉네임 반환")
	@GetMapping("/nickname/random")
	public ResponseEntity<ApiResponse<RandomNicknameResponse>> getRandomNickname() {
		String randomNickname = userService.getRandomNickname();
		return ResponseEntity.ok(ApiResponse.ok(RandomNicknameResponse.from(randomNickname)));
	}
}
