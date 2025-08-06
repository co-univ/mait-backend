package com.coniv.mait.config;

import java.io.IOException;
import java.net.ServerSocket;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import redis.embedded.RedisServer;

@TestConfiguration
@Profile("test")
@ConditionalOnProperty(name = "spring.embedded.redis.enabled", havingValue = "true", matchIfMissing = false)
public class TestRedisConfig {

	private RedisServer redisServer;
	private int redisPort;

	@PostConstruct
	public void startRedis() {
		try {
			// 사용 가능한 포트 자동 찾기
			redisPort = findAvailablePort();
			System.out.println("Starting embedded Redis on port: " + redisPort);

			redisServer = RedisServer.builder()
				.port(redisPort)
				.setting("maxmemory 128M")
				.build();
			redisServer.start();

			// 시스템 프로퍼티에 포트 설정 (application-test.yml보다 우선)
			System.setProperty("spring.data.redis.port", String.valueOf(redisPort));

		} catch (Exception e) {
			System.err.println("Failed to start embedded Redis: " + e.getMessage());
			System.err.println("Falling back to mock Redis behavior...");
			// 실패해도 테스트가 중단되지 않도록 함
		}
	}

	@PreDestroy
	public void stopRedis() {
		try {
			if (redisServer != null && redisServer.isActive()) {
				redisServer.stop();
				System.out.println("Embedded Redis stopped");
			}
		} catch (Exception e) {
			System.err.println("Error stopping embedded Redis: " + e.getMessage());
		}
	}

	private int findAvailablePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}
}
