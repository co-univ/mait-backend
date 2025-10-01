package com.coniv.mait.web.question.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageInfoApiResponse(
	@Schema(description = "생성된 이미지 url", requiredMode = Schema.RequiredMode.REQUIRED)
	String imageUrl
) {
	public static ImageInfoApiResponse from(String imageUrl) {
		return new ImageInfoApiResponse(imageUrl);
	}
}
