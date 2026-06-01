package org.example.selfwebblog.service.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class UploadStorageService {

    private final Path uploadRoot;
    private final ImageValidator imageValidator;

    public UploadStorageService(
        @Value("${upload.path}") String uploadPath,
        ImageValidator imageValidator
    ) {
        this.uploadRoot = Path.of(uploadPath).toAbsolutePath().normalize();
        this.imageValidator = imageValidator;
    }

    public StoredUpload storeImage(MultipartFile file, UploadTarget target) throws IOException {
        ImageValidation validation = imageValidator.validate(file);
        Path targetDirectory = resolveTargetDirectory(target);
        Files.createDirectories(targetDirectory);

        String filename = UUID.randomUUID() + "." + validation.extension();
        Path destination = targetDirectory.resolve(filename).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new IOException("上传目录不安全");
        }

        file.transferTo(destination.toFile());
        return new StoredUpload(filename, target.urlPrefix() + filename, validation.extension(), validation.size());
    }

    private Path resolveTargetDirectory(UploadTarget target) throws IOException {
        Path directory = target.subdirectory().isBlank()
            ? uploadRoot
            : uploadRoot.resolve(target.subdirectory());
        Path normalized = directory.toAbsolutePath().normalize();
        if (!normalized.startsWith(uploadRoot)) {
            throw new IOException("上传目录不安全");
        }
        return normalized;
    }
}
