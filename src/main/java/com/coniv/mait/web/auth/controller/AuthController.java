package com.coniv.mait.web.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.auth.service.AuthService;
import com.coniv.mait.global.jwt.Token;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.global.util.CookieUtil;
import com.coniv.mait.web.auth.dto.LoginApiRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "회원 인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private static final String AUTH_HEADER = "Authorization";

	private static final String BEARER_TOKEN = "Bearer ";

	private final AuthService authService;

	@Operation(summary = "로그인 API", description = "사용자 로그인 API")
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<Void>> login(@RequestBody @Valid LoginApiRequest request,
		HttpServletResponse httpServletResponse) {
		Token token = authService.login(request.email(), request.password());

		httpServletResponse.addHeader(AUTH_HEADER, BEARER_TOKEN + token.accessToken());
		Cookie refreshTokenCookie = CookieUtil.createRefreshCookie(token.refreshToken());
		httpServletResponse.addCookie(refreshTokenCookie);

		return ResponseEntity.ok(ApiResponse.noContent());
	}

	@Operation(summary = "Access token 반환 API", description = "Oauth 로그인 후 access token을 반환하는 API")
	@GetMapping("/access-token")
	public ResponseEntity<ApiResponse<String>> getAccessToken(@RequestParam("code") String code) {
		return ResponseEntity.ok(ApiResponse.ok(authService.getAccessTokenFromCode(code)));
	}
}
