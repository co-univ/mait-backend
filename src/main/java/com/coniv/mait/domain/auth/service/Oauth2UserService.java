package com.coniv.mait.domain.auth.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.coniv.mait.domain.auth.oauth.GoogleUserDetails;
import com.coniv.mait.domain.auth.oauth.OAuth2UserInfo;
import com.coniv.mait.domain.auth.oauth.Oauth2UserDetails;
import com.coniv.mait.domain.auth.repository.UserEntityRepository;
import com.coniv.mait.domain.user.entity.UserEntity;
import com.coniv.mait.domain.user.enums.LoginProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class Oauth2UserService extends DefaultOAuth2UserService {

	private final UserEntityRepository userEntityRepository;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);

		String provider = userRequest.getClientRegistration().getRegistrationId();
		LoginProvider loginProvider = LoginProvider.findByProvider(provider);
		OAuth2UserInfo oAuth2UserInfo = null;

		if (provider.equals("google")) {
			oAuth2UserInfo = new GoogleUserDetails(oAuth2User.getAttributes());
		}
		String providerId = oAuth2UserInfo.getProviderId();
		String email = oAuth2UserInfo.getEmail();
		String loginId = provider + "_" + providerId;
		String name = oAuth2UserInfo.getName();

		UserEntity user = userEntityRepository.findByProviderId(loginId)
			.orElseGet(() -> {
				UserEntity newUser = UserEntity.socialLoginUser(
					email, name, providerId, loginProvider
				);
				return userEntityRepository.save(newUser);
			});

		return new Oauth2UserDetails(user, oAuth2User.getAttributes());
	}
}
