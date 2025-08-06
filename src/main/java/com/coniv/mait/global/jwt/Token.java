package com.coniv.mait.global.jwt;

import lombok.Builder;

@Builder
public record Token(String accessToken, String refreshToken) {
}
