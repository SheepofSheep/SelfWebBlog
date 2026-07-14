package org.example.selfwebblog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "blog.data-dir=target/test-data/context")
class SelfWebBlogApplicationTests {

	@Test
	void contextLoads() {
	}

}
