package com.threadia.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.threadia.backend.platform.bootstrap.PlatformAdminBootstrap;

@SpringBootTest(properties = {
		"spring.jpa.hibernate.ddl-auto=none",
		"threadia.jwt.secret=01234567890123456789012345678901"
})
class BackendApplicationTests {

	@MockitoBean
	private PlatformAdminBootstrap platformAdminBootstrap;

	@Test
	void contextLoads() {
	}

}
