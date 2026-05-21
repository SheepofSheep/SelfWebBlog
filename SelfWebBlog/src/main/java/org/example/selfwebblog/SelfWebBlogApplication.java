package org.example.selfwebblog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.selfwebblog.mapper")
public class SelfWebBlogApplication {

	public static void main(String[] args) {
		SpringApplication.run(SelfWebBlogApplication.class, args);
	}

}
