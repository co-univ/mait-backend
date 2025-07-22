package com.coniv.mait.domain.user.enums;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoginProvider {

	GOOGLE("google", "구글");

	private final String provider;
	private final String description;

	public static LoginProvider findByProvider(String provider) {
		return Arrays.stream(values())
			.filter(lp -> lp.provider.equals(provider))
			.findFirst()
			.orElseThrow(() ->
				new IllegalArgumentException("지원되지 않는 LoginProvider: " + provider));
	}
}
