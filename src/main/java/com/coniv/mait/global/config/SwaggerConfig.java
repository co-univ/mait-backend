package com.coniv.mait.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;

@Configuration
@Profile({"dev", "local"})
@RequiredArgsConstructor
public class SwaggerConfig {

	@Value("${server.domain}")
	private String host;

	@Bean
	public OpenAPI openApi() {
		Server httpsServer = new Server();
		httpsServer.setUrl(host);

		return new OpenAPI()
			.addServersItem(httpsServer)
			.info(new Info()
				.title("MAIT API Documentation")
				.description("교육 문제 생성 및 풀이 플랫폼 API")
				.version("v1.0.0"));
	}
}
