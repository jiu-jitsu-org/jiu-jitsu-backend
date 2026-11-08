package com.jiujitsu.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestFcmConfig.class)
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
