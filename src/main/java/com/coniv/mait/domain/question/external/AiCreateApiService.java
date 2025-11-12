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

	/**
	 * AI 서버에 문제 생성 요청
	 * 
	 * @param request AI 생성 요청 DTO
	 * @return AI 생성 응답 (1-2분 소요)
	 * @apiNote Timeout: 5분 (AI 서버 응답 대기 시간 고려)
	 */
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
