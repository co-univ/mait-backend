package com.coniv.mait.domain.user.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginProvider {

	GOOGLE("google", "구글");

	private final String provider;
	private final String description;

	private static final Map<String, LoginProvider> PROVIDER_MAP =
		Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(
			LoginProvider::getProvider, Function.identity()
		));

	public static LoginProvider findByProvider(String provider) {
		LoginProvider loginProvider = PROVIDER_MAP.get(provider);
		if (loginProvider == null) {
			throw new IllegalArgumentException("지원되지 않는 LoginProvider: " + provider);
		}
		return loginProvider;
	}
}
