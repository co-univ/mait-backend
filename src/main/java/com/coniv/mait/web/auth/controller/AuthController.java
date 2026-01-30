package com.coniv.mait.web.auth.controller;

import static com.coniv.mait.global.auth.jwt.constant.TokenConstants.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coniv.mait.domain.auth.service.AuthService;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.auth.cookie.CookieFactory;
import com.coniv.mait.global.auth.jwt.Token;
import com.coniv.mait.global.response.ApiResponse;
import com.coniv.mait.web.auth.dto.LoginApiRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
	private final CookieFactory cookieFactory;

	@Operation(summary = "로그인 API", description = "사용자 로그인 API")
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<Void>> login(@RequestBody @Valid LoginApiRequest request) {
		Token token = authService.login(request.email(), request.password());

		return ResponseEntity.ok()
			.header("Set-Cookie", cookieFactory.createRefreshResponseCookie(token.refreshToken()).toString())
			.header(AUTH_HEADER, token.accessToken())
			.body(ApiResponse.noContent());
	}

	@Operation(summary = "로그아웃 API", description = "사용자 로그아웃 API")
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
		@AuthenticationPrincipal UserEntity user,
		@Parameter(hidden = true) @RequestHeader(value = AUTH_HEADER) String authorizationHeader,
		@Parameter(hidden = true) @CookieValue(name = REFRESH_TOKEN) String refreshToken) {

		String accessToken = authorizationHeader.substring(BEARER_TOKEN.length()).trim();
		authService.logout(user, accessToken, refreshToken);

		return ResponseEntity.noContent()
			.header("Set-Cookie", cookieFactory.createExpiredRefreshResponseCookie().toString())
			.build();
	}

	@Operation(summary = "토큰 재발급 API", description = "토큰 재발급 API")
	@PostMapping("/reissue")
	public ResponseEntity<ApiResponse<Void>> reissue(
		@Parameter(hidden = true) @CookieValue(name = REFRESH_TOKEN) String refreshToken) {

		Token newToken = authService.reissue(refreshToken);

		return ResponseEntity.ok()
			.header("Set-Cookie", cookieFactory.createRefreshResponseCookie(newToken.refreshToken()).toString())
			.header(AUTH_HEADER, newToken.accessToken())
			.body(ApiResponse.noContent());
	}

	@Operation(summary = "Access token 반환 API", description = "Oauth 로그인 후 access token을 반환하는 API")
	@GetMapping("/access-token")
	public ResponseEntity<ApiResponse<String>> getAccessToken(@RequestParam("code") String code) {
		return ResponseEntity.ok(ApiResponse.ok(authService.getAccessTokenFromCode(code)));
	}
}
