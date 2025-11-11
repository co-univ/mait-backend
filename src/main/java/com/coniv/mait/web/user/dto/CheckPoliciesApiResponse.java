package com.coniv.mait.web.user.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record CheckPoliciesApiResponse(

	@Schema(description = "성공 메시지", requiredMode = Schema.RequiredMode.REQUIRED)
	String message
) {
	public static CheckPoliciesApiResponse of(LocalDateTime localDateTime) {
		return new CheckPoliciesApiResponse(
			String.format("%d시 %d분 부로 정책이 완료되었습니다.",
				localDateTime.getHour(),
				localDateTime.getMinute()
			));
	}
}

