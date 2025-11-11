package com.coniv.mait.domain.question.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.coniv.mait.domain.question.external.dto.AiCreateRequest;
import com.coniv.mait.domain.question.external.dto.AiCreateResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiCreateApiService {

	@Value("${api.ai.url}")
	private String apiUrl;

	private final WebClient webClient;

	public AiCreateResponse createQuestionSet(final AiCreateRequest request) {
		return webClient.post()
			.uri(apiUrl + "/api/ai/generate")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(request)
			.retrieve()
			.bodyToMono(AiCreateResponse.class)
			.block();
	}
}
