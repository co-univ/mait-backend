package com.coniv.mait.domain.question.external.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;

@Builder
public record AiCreateRequest(
	String subject,

	String difficulty,

	List<String> urls,

	String instruction,

	Map<String, Integer> counts
) {
}
