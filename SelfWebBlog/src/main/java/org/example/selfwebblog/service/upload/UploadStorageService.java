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
        return storeImage(file, target, "");
    }

    public StoredUpload storeAvatar(MultipartFile file, Long ownerId) throws IOException {
        if (ownerId == null || ownerId <= 0) throw new IllegalArgumentException("头像上传者无效");
        return storeImage(file, UploadTarget.AVATAR, ownerId + "-");
    }

    private StoredUpload storeImage(MultipartFile file, UploadTarget target, String ownerPrefix) throws IOException {
        ImageValidation validation = imageValidator.validate(file);
        Path targetDirectory = resolveTargetDirectory(target);
        Files.createDirectories(targetDirectory);

        String filename = ownerPrefix + UUID.randomUUID() + "." + validation.extension();
        Path destination = targetDirectory.resolve(filename).normalize();
        if (!destination.startsWith(uploadRoot)) {
            throw new IOException("上传目录不安全");
        }

        file.transferTo(destination.toFile());
        return new StoredUpload(filename, target.urlPrefix() + filename, validation.extension(), validation.size());
    }

    public void deleteOwnedAvatar(String url, Long ownerId) {
        if (ownerId == null || ownerId <= 0 || url == null
                || !url.startsWith(UploadTarget.AVATAR.urlPrefix())) return;
        String filename = url.substring(UploadTarget.AVATAR.urlPrefix().length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\")) return;
        String prefix = ownerId + "-";
        // Legacy UUID-only files have no ownership proof and must not be deleted here.
        if (!filename.startsWith(prefix)) return;
        int extension = filename.lastIndexOf('.');
        if (extension <= prefix.length()) return;
        String identifier = filename.substring(prefix.length(), extension);
        try {
            if (!UUID.fromString(identifier).toString().equalsIgnoreCase(identifier)) return;
        } catch (IllegalArgumentException exception) {
            return;
        }
        Path avatarRoot = uploadRoot.resolve(UploadTarget.AVATAR.subdirectory()).normalize();
        Path candidate = avatarRoot.resolve(filename).normalize();
        if (!candidate.startsWith(avatarRoot)) return;
        try {
            Files.deleteIfExists(candidate);
        } catch (IOException ignored) {
            // A stale avatar is preferable to failing a successful profile update.
        }
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
