package com.coniv.mait.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.coniv.mait.domain.solve.service.component.ScorerLuaScript;
import com.coniv.mait.domain.solve.service.component.ScorerProcessor;

import static org.mockito.Mockito.*;

@TestConfiguration
@Profile("test")
public class TestRedisConfigWithoutServer {

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