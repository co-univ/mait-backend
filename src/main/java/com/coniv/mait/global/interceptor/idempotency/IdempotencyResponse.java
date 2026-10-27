package com.coniv.mait.global.interceptor.idempotency;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.http.HttpServletResponse;

public record IdempotencyResponse(int status, Map<String, List<String>> headers, byte[] body) {

	private static final List<String> CACHED_HEADERS = List.of(
		"Content-Type", "Content-Language", "Location", "ETag", "Cache-Control");

	public static IdempotencyResponse from(ContentCachingResponseWrapper response) {
		Map<String, List<String>> headers = new LinkedHashMap<>();
		for (String name : CACHED_HEADERS) {
			if (response.containsHeader(name)) {
				headers.put(name, List.copyOf(response.getHeaders(name)));
			}
		}
		return new IdempotencyResponse(response.getStatus(), headers, response.getContentAsByteArray());
	}

	public void writeTo(HttpServletResponse response) throws IOException {
		response.setStatus(status);
		headers.forEach((name, values) -> {
			response.setHeader(name, values.getFirst());
			values.stream().skip(1).forEach(value -> response.addHeader(name, value));
		});
		response.getOutputStream().write(body);
	}
}
