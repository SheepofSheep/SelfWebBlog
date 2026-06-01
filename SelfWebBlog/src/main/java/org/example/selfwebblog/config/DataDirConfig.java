package org.example.selfwebblog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;

@Configuration
public class DataDirConfig {

    private static final Logger log = LoggerFactory.getLogger(DataDirConfig.class);

    @Value("${blog.data-dir}")
    private String dataDir;

    @Value("${upload.path}")
    private String uploadDir;

    @Bean
    public static BeanFactoryPostProcessor ensureDataDirsBeforeDataSource(Environment environment) {
        return beanFactory -> {
            ensureDir(environment.getProperty("blog.data-dir"));
            ensureDir(environment.getProperty("upload.path"));
        };
    }

    @Bean
    public CommandLineRunner ensureDataDirs() {
        return args -> {
            ensureDir(dataDir);
            ensureDir(uploadDir);
            log.info("数据目录已就绪: {}", dataDir);
        };
    }

    private static void ensureDir(String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) log.info("已创建目录: {}", path);
            else log.warn("无法创建目录: {}", path);
        }
    }
}
