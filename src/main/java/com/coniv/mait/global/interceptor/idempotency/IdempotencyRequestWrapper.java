package com.coniv.mait.global.interceptor.idempotency;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.Getter;

@Getter
public class IdempotencyRequestWrapper extends HttpServletRequestWrapper {

	private final byte[] body;

	public IdempotencyRequestWrapper(HttpServletRequest request) throws IOException {
		super(request);
		body = request.getInputStream().readAllBytes();
	}

	@Override
	public ServletInputStream getInputStream() {
		ByteArrayInputStream stream = new ByteArrayInputStream(body);
		return new ServletInputStream() {
			@Override
			public int read() {
				return stream.read();
			}

			@Override
			public boolean isFinished() {
				return stream.available() == 0;
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener listener) {
				throw new UnsupportedOperationException("멱등성 요청은 동기 처리만 지원합니다.");
			}
		};
	}

	@Override
	public BufferedReader getReader() {
		String encoding = getCharacterEncoding();
		return new BufferedReader(new InputStreamReader(getInputStream(),
			encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding)));
	}
}
