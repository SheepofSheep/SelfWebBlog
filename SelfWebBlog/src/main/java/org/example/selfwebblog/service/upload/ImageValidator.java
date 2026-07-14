package org.example.selfwebblog.service.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ImageValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Map<String, Set<String>> EXTENSION_MIME_TYPES = Map.of(
        "jpg", Set.of("image/jpeg"),
        "jpeg", Set.of("image/jpeg"),
        "png", Set.of("image/png"),
        "gif", Set.of("image/gif"),
        "webp", Set.of("image/webp")
    );

    private final long maxImageBytes;
    private final int maxWidth;
    private final int maxHeight;

    public ImageValidator(
        @Value("${upload.max-image-bytes:10485760}") long maxImageBytes,
        @Value("${upload.max-width:6000}") int maxWidth,
        @Value("${upload.max-height:6000}") int maxHeight
    ) {
        this.maxImageBytes = maxImageBytes;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public ImageValidation validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的文件");
        }
        if (file.getSize() > maxImageBytes) {
            throw new IllegalArgumentException("图片不能超过 " + (maxImageBytes / 1024 / 1024) + "MB");
        }

        String claimedExtension = getClaimedExtension(file.getOriginalFilename());
        validateMimeType(file.getContentType(), claimedExtension);

        String detectedExtension = detectImageType(file);
        if (detectedExtension == null) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 jpg、png、gif、webp");
        }
        if (!sameImageFamily(claimedExtension, detectedExtension)) {
            throw new IllegalArgumentException("图片扩展名与实际文件类型不一致");
        }

        validateDimensions(file, detectedExtension);
        return new ImageValidation(normalizeExtension(detectedExtension), file.getContentType(), file.getSize());
    }

    private String getClaimedExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件名格式不正确");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1)
            .toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("不支持的文件扩展名，仅支持 jpg、png、gif、webp");
        }
        return normalizeExtension(extension);
    }

    private void validateMimeType(String contentType, String extension) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("图片 MIME 类型不正确");
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (!EXTENSION_MIME_TYPES.getOrDefault(extension, Set.of()).contains(normalized)) {
            throw new IllegalArgumentException("图片 MIME 类型与扩展名不一致");
        }
    }

    private String detectImageType(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = in.read(header);
            if (read < 4) return null;

            String hex = HexFormat.of().withUpperCase().formatHex(header, 0, Math.min(read, 4));
            if (hex.startsWith("FFD8FF")) return "jpg";
            if (hex.startsWith("89504E47")) return "png";
            if (hex.startsWith("47494638")) return "gif";
            if (read >= 12 && isWebp(header)) return "webp";
            return null;
        } catch (IOException e) {
            throw new IllegalArgumentException("无法读取图片文件");
        }
    }

    private boolean isWebp(byte[] header) {
        String riff = new String(header, 0, 4);
        String webp = new String(header, 8, 4);
        return "RIFF".equals(riff) && "WEBP".equals(webp);
    }

    private void validateDimensions(MultipartFile file, String extension) {
        try (InputStream in = file.getInputStream();
             ImageInputStream imageInput = ImageIO.createImageInputStream(in)) {
            if (imageInput == null) throw new IllegalArgumentException("无法解析图片尺寸");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("无法解析图片尺寸");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) throw new IllegalArgumentException("图片尺寸不正确");
                if (width > maxWidth || height > maxHeight) {
                    throw new IllegalArgumentException("图片尺寸不能超过 " + maxWidth + "x" + maxHeight);
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw new IllegalArgumentException("无法完整解析图片");
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("无法解析图片尺寸");
        }
    }

    private boolean sameImageFamily(String claimedExtension, String detectedExtension) {
        return normalizeExtension(claimedExtension).equals(normalizeExtension(detectedExtension));
    }

    private String normalizeExtension(String extension) {
        return "jpeg".equals(extension) ? "jpg" : extension;
    }
}
