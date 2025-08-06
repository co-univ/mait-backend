package com.coniv.mait.web.integration;

import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.coniv.mait.config.TestRedisConfigWithoutServer;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@Disabled
@AutoConfigureMockMvc
@Rollback
@Transactional
@ActiveProfiles({"test"})
@Import(TestRedisConfigWithoutServer.class)
public class BaseIntegrationTest {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;
}
