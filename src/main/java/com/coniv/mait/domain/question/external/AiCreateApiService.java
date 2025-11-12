package com.coniv.mait.domain.question.external;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.coniv.mait.domain.question.external.dto.AiCreateRequest;
import com.coniv.mait.domain.question.external.dto.AiCreateResponse;

@Service
public class AiCreateApiService {

	@Value("${api.ai.url}")
	private String apiUrl;

	private final WebClient aiWebClient;

	public AiCreateApiService(@Qualifier("aiWebClient") WebClient aiWebClient) {
		this.aiWebClient = aiWebClient;
	}

	public AiCreateResponse createQuestionSet(final AiCreateRequest request) {
		return aiWebClient.post()
			.uri(apiUrl + "/api/ai/generate")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(request)
			.retrieve()
			.bodyToMono(AiCreateResponse.class)
			.block(Duration.ofMinutes(5));  // ⏱️ 5분 타임아웃
	}
}
