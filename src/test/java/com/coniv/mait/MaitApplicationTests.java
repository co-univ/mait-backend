package com.coniv.mait;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.coniv.mait.config.TestRedisConfigWithoutServer;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfigWithoutServer.class)
class MaitApplicationTests {

	@Test
	void contextLoads() {
	}

}
