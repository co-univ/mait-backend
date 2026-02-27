package com.coniv.mait.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
@Profile({"dev", "local"})
public class SwaggerConfig {

	private static final String BEARER_AUTH = "Bearer Authentication";

	@Value("${server.domain}")
	private String host;

	@Bean
	public OpenAPI openApi() {
		Server httpsServer = new Server();
		httpsServer.setUrl(host);

		SecurityScheme bearerScheme = new SecurityScheme()
			.type(SecurityScheme.Type.HTTP)
			.scheme("bearer")
			.bearerFormat("JWT")
			.in(SecurityScheme.In.HEADER)
			.name("Authorization");

		return new OpenAPI()
			.addServersItem(httpsServer)
			.components(new Components().addSecuritySchemes(BEARER_AUTH, bearerScheme))
			.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
			.info(new Info()
				.title("MAIT API Documentation")
				.description("교육 문제 생성 및 풀이 플랫폼 API")
				.version("v1.0.0"));
	}
}
