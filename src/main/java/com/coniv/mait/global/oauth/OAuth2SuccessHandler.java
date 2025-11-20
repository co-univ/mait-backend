package com.coniv.mait.global.oauth;

import static com.coniv.mait.global.jwt.constant.TokenConstants.*;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.coniv.mait.domain.auth.dto.OauthPendingPayload;
import com.coniv.mait.domain.auth.enums.PendingSignupKey;
import com.coniv.mait.domain.auth.oauth.Oauth2UserDetails;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.global.jwt.JwtTokenProvider;
import com.coniv.mait.global.jwt.RefreshToken;
import com.coniv.mait.global.jwt.Token;
import com.coniv.mait.global.jwt.cache.OauthAccessCodeRedisRepository;
import com.coniv.mait.global.jwt.cache.OauthPendingRedisRepository;
import com.coniv.mait.global.jwt.repository.RefreshTokenRepository;
import com.coniv.mait.global.oauth.constant.AuthConstant;
import com.coniv.mait.global.util.Base62Convertor;
import com.coniv.mait.global.util.CookieUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthConstant authConstant;
	private final CookieUtil cookieUtil;
	private final OauthAccessCodeRedisRepository oauthAccessCodeRedisRepository;
	private final OauthPendingRedisRepository oauthPendingRedisRepository;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
		Authentication authentication) throws IOException {

		Oauth2UserDetails oauthDetails = (Oauth2UserDetails)authentication.getPrincipal();
		UserEntity user = oauthDetails.getUser();

		if (oauthDetails.getPendingSignupKey() == PendingSignupKey.SIGNUP) {
			OauthPendingPayload payload = OauthPendingPayload.builder()
				.provider(user.getLoginProvider().toString())
				.providerId((user.getProviderId()))
				.email(oauthDetails.getUser().getEmail())
				.name(oauthDetails.getUser().getName())
				.build();

			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(payload);

			String signupKey = Base62Convertor.uuidToBase62(UUID.randomUUID());
			oauthPendingRedisRepository.save(signupKey, json);

			response.addCookie(cookieUtil.createOauthSignupCookie(signupKey));
			response.sendRedirect(authConstant.getOauthSignupUrl());
			return;
		}

		if (oauthDetails.getPendingSignupKey() == PendingSignupKey.LOGIN) {
			Token token = jwtTokenProvider.createToken(user.getId());

			String accessToken = token.accessToken();
			response.addHeader(ACCESS_TOKEN, accessToken);

			String code = UUID.randomUUID().toString();
			oauthAccessCodeRedisRepository.save(code, accessToken);

			RefreshToken refreshToken = new RefreshToken(user.getId(), token.refreshToken());
			refreshTokenRepository.save(refreshToken);

			response.addHeader("Set-Cookie", cookieUtil.createRefreshResponseCookie(token.refreshToken()).toString());

			response.sendRedirect(authConstant.getOAuthSuccessRedirectUrl() + "?code=" + code);
			return;
		}

		throw new IllegalStateException("Unexpected pendingSignupKey: " + oauthDetails.getPendingSignupKey());
	}
}
