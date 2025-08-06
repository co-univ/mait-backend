package com.coniv.mait;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.coniv.mait.domain.solve.service.component.ScorerLuaScript;
import com.coniv.mait.domain.solve.service.component.ScorerProcessor;

import static org.mockito.Mockito.*;

@SpringBootApplication
@Import(TestMaitApplication.TestRedisConfiguration.class)
public class TestMaitApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestMaitApplication.class, args);
	}

	@TestConfiguration
	static class TestRedisConfiguration {

		@Bean
		@Primary
		public RedisTemplate<String, Object> mockRedisTemplate() {
			return mock(RedisTemplate.class);
		}

		@Bean
		@Primary
		public StringRedisTemplate mockStringRedisTemplate() {
			return mock(StringRedisTemplate.class);
		}

		@Bean
		@Primary
		public DefaultRedisScript<String> mockWinnerLuaScript() {
			DefaultRedisScript<String> script = new DefaultRedisScript<>();
			script.setScriptText(ScorerLuaScript.SCORER_CHECK_LUA_SCRIPT);
			script.setResultType(String.class);
			return script;
		}

		@Bean
		@Primary
		public ScorerProcessor mockScorerProcessor() {
			ScorerProcessor mock = mock(ScorerProcessor.class);
			// 기본 동작: 첫 번째 사용자를 득점자로 반환
			when(mock.getScorer(anyLong(), anyLong(), anyLong()))
				.thenAnswer(invocation -> invocation.getArgument(1)); // userId 반환
			return mock;
		}
	}
}