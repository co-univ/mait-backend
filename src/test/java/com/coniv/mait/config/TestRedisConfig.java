package com.coniv.mait.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import redis.embedded.RedisServer;

@TestConfiguration
@Profile("test")
public class TestRedisConfig {

	private RedisServer redisServer;

	@PostConstruct
	public void startRedis() {
		try {
			redisServer = new RedisServer(6379);
			redisServer.start();
		} catch (Exception e) {
			// 이미 Redis가 실행 중인 경우 무시
			System.out.println("Redis server already running or failed to start: " + e.getMessage());
		}
	}

	@PreDestroy
	public void stopRedis() {
		if (redisServer != null && redisServer.isActive()) {
			redisServer.stop();
		}
	}
}
