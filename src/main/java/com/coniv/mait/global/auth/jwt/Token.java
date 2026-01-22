package com.coniv.mait.global.auth.jwt;

import lombok.Builder;

@Builder
public record Token(String accessToken, String refreshToken) {
}
