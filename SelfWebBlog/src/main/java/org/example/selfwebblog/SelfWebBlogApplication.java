package org.example.selfwebblog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
		"org.example.selfwebblog.mapper",
		"org.example.selfwebblog.interaction.mapper",
		"org.example.selfwebblog.analytics",
		"org.example.selfwebblog.content",
		"org.example.selfwebblog.upload"
		,"org.example.selfwebblog.admin"
})
public class SelfWebBlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(SelfWebBlogApplication.class, args);
	}

}
