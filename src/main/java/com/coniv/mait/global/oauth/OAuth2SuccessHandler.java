package com.coniv.mait.global.oauth;

import static com.coniv.mait.global.jwt.constant.TokenConstants.*;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.auth.oauth.Oauth2UserDetails;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.jwt.JwtTokenProvider;
import com.coniv.mait.global.jwt.RefreshToken;
import com.coniv.mait.global.jwt.Token;
import com.coniv.mait.global.jwt.cache.OauthAccessCodeRedisRepository;
import com.coniv.mait.global.jwt.repository.RefreshTokenRepository;
import com.coniv.mait.global.oauth.constant.AuthConstant;
import com.coniv.mait.global.util.CookieUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthConstant authConstant;
	private final OauthAccessCodeRedisRepository oauthAccessCodeRedisRepository;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		Oauth2UserDetails oauthDetails = (Oauth2UserDetails)authentication.getPrincipal();
		UserEntity user = oauthDetails.getUser();
		Token token = jwtTokenProvider.createToken(user.getId());

		String accessToken = token.accessToken();
		response.addHeader(ACCESS_TOKEN, accessToken);

		String code = UUID.randomUUID().toString();
		oauthAccessCodeRedisRepository.save(code, accessToken);

		RefreshToken refreshToken = new RefreshToken(user.getId(), token.refreshToken());
		refreshTokenRepository.save(refreshToken);

		Cookie cookie = CookieUtil.createRefreshCookie(token.refreshToken());
		response.addCookie(cookie);
		response.sendRedirect(authConstant.getOAuthSuccessRedirectUrl() + "?code=" + code);
	}
}
