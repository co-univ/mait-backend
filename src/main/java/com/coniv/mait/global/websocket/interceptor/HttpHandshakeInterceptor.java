package com.coniv.mait.global.websocket.interceptor;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class HttpHandshakeInterceptor implements HandshakeInterceptor {

	private static final String REQUEST_ID_HEADER = "requestId";
	private static final String X_REQUEST_ID_HEADER = "X-Request-ID";

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
		Map<String, Object> attributes) {

		HttpHeaders headers = request.getHeaders();

		String requestId = headers.getFirst(X_REQUEST_ID_HEADER);

		if (requestId != null && !requestId.isEmpty()) {
			attributes.put(REQUEST_ID_HEADER, requestId);
		}

		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
		Exception exception) {
	}
}
