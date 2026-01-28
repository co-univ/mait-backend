package com.coniv.mait.global.config;

import java.util.Map;
import java.util.concurrent.Executor;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
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
		taskExecutor.setTaskDecorator(new MdcTaskDecorator());
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
		taskExecutor.setTaskDecorator(new MdcTaskDecorator());
		taskExecutor.initialize();
		return taskExecutor;
	}

	public static class MdcTaskDecorator implements TaskDecorator {
		@Override
		public Runnable decorate(Runnable runnable) {
			Map<String, String> contextMap = MDC.getCopyOfContextMap();
			return () -> {
				try {
					if (contextMap != null) {
						MDC.setContextMap(contextMap);
					}
					runnable.run();
				} finally {
					MDC.clear();
				}
			};
		}
	}
}
