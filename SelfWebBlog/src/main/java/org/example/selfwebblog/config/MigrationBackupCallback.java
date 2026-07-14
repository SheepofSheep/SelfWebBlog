package org.example.selfwebblog.config;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Component
public class MigrationBackupCallback implements Callback {
    private static final Logger log = LoggerFactory.getLogger(MigrationBackupCallback.class);
    private static final DateTimeFormatter NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path dataDirectory;

    public MigrationBackupCallback(@Value("${blog.data-dir}") String dataDirectory) {
        this.dataDirectory = Path.of(dataDirectory).toAbsolutePath().normalize();
    }

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return false;
    }

    @Override
    public void handle(Event event, Context context) {
        try {
            Integer tableCount;
            try (var statement = context.getConnection().prepareStatement(
                    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'blog_info'")) {
                try (var result = statement.executeQuery()) {
                    tableCount = result.next() ? result.getInt(1) : 0;
                }
            }
            if (tableCount == 0) return;

            Path backupDirectory = dataDirectory.resolve("backups").normalize();
            if (!backupDirectory.startsWith(dataDirectory)) throw new IllegalStateException("备份目录无效");
            Files.createDirectories(backupDirectory);
            Path backup = backupDirectory.resolve(
                    "before-migrate-" + LocalDateTime.now().format(NAME_TIME) + ".db");
            String escaped = backup.toString().replace("'", "''");
            try (var statement = context.getConnection().createStatement()) {
                statement.execute("VACUUM INTO '" + escaped + "'");
            }
            rotate(backupDirectory, 8);
            log.info("数据库迁移前备份完成: {}", backup);
        } catch (Exception exception) {
            throw new IllegalStateException("数据库迁移前备份失败，已停止迁移", exception);
        }
    }

    private void rotate(Path directory, int keep) throws Exception {
        try (var files = Files.list(directory)) {
            Path[] backups = files
                    .filter(path -> path.getFileName().toString().startsWith("before-migrate-"))
                    .sorted(Comparator.comparingLong(this::lastModified).reversed())
                    .toArray(Path[]::new);
            for (int index = keep; index < backups.length; index++) Files.deleteIfExists(backups[index]);
        }
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Override
    public String getCallbackName() {
        return "sqlite-migration-backup";
    }
}
