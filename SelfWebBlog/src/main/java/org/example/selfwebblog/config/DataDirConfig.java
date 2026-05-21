package org.example.selfwebblog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class DataDirConfig {

    private static final Logger log = LoggerFactory.getLogger(DataDirConfig.class);

    @Value("${blog.data-dir}")
    private String dataDir;

    @Value("${upload.path}")
    private String uploadDir;

    @Bean
    public CommandLineRunner ensureDataDirs() {
        return args -> {
            ensureDir(dataDir);
            ensureDir(uploadDir);
            log.info("数据目录已就绪: {}", dataDir);
        };
    }

    private void ensureDir(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) log.info("已创建目录: {}", path);
            else log.warn("无法创建目录: {}", path);
        }
    }
}
