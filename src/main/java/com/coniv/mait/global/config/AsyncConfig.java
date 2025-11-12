package com.coniv.mait.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean("maitThreadPoolExecutor")
	public Executor maitThreadPoolExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(20);
		taskExecutor.setMaxPoolSize(100);
		taskExecutor.setQueueCapacity(10000);
		taskExecutor.setThreadNamePrefix("mait-thread");
		taskExecutor.initialize();
		return taskExecutor;
	}

	@Bean("aiUploadThreadPoolExecutor")
	public Executor aiUploadThreadPoolExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(20);
		taskExecutor.setMaxPoolSize(100);
		taskExecutor.setQueueCapacity(10000);
		taskExecutor.setThreadNamePrefix("mait-ai");
		taskExecutor.initialize();
		return taskExecutor;
	}
}
