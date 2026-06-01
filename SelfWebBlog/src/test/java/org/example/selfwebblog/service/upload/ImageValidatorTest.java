package org.example.selfwebblog.service.upload;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageValidatorTest {

    @Test
    void acceptsValidPng() throws Exception {
        ImageValidator validator = new ImageValidator(1024 * 1024, 100, 100);
        MockMultipartFile file = imageFile("avatar.png", "image/png", 1, 1);

        ImageValidation validation = validator.validate(file);

        assertThat(validation.extension()).isEqualTo("png");
        assertThat(validation.contentType()).isEqualTo("image/png");
        assertThat(validation.size()).isGreaterThan(0);
    }

    @Test
    void rejectsSpoofedImageContent() {
        ImageValidator validator = new ImageValidator(1024 * 1024, 100, 100);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "fake.png",
            "image/png",
            "not an image".getBytes()
        );

        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("不支持的文件格式");
    }

    @Test
    void rejectsMimeMismatch() throws Exception {
        ImageValidator validator = new ImageValidator(1024 * 1024, 100, 100);
        MockMultipartFile file = imageFile("avatar.png", "image/jpeg", 1, 1);

        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MIME");
    }

    @Test
    void rejectsImagesOverPixelLimit() throws Exception {
        ImageValidator validator = new ImageValidator(1024 * 1024, 1, 1);
        MockMultipartFile file = imageFile("large.png", "image/png", 2, 2);

        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("图片尺寸不能超过");
    }

    @Test
    void storesImageInTargetDirectory(@TempDir Path tempDir) throws Exception {
        ImageValidator validator = new ImageValidator(1024 * 1024, 100, 100);
        UploadStorageService service = new UploadStorageService(tempDir.toString(), validator);
        MockMultipartFile file = imageFile("avatar.png", "image/png", 1, 1);

        StoredUpload upload = service.storeImage(file, UploadTarget.AVATAR);

        assertThat(upload.url()).startsWith("/uploads/avatars/");
        assertThat(Files.exists(tempDir.resolve("avatars").resolve(upload.filename()))).isTrue();
    }

    private MockMultipartFile imageFile(String filename, String contentType, int width, int height) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", output);
        return new MockMultipartFile("file", filename, contentType, output.toByteArray());
    }
}
