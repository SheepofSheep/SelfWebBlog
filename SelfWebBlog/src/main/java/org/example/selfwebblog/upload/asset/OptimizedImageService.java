package org.example.selfwebblog.upload.asset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Service
public class OptimizedImageService {
    private static final Set<Integer> ALLOWED_WIDTHS = Set.of(160, 480, 1280);

    private final Path uploadRoot;
    private final ImageVariantService variantService;

    public OptimizedImageService(
            @Value("${upload.path}") String uploadPath,
            ImageVariantService variantService) {
        this.uploadRoot = Path.of(uploadPath).toAbsolutePath().normalize();
        this.variantService = variantService;
    }

    public synchronized Path optimize(String uploadUrl, int width) throws IOException {
        if (!ALLOWED_WIDTHS.contains(width)) {
            throw new IllegalArgumentException("不支持的图片尺寸");
        }
        if (uploadUrl == null || !uploadUrl.startsWith("/uploads/")) {
            throw new IllegalArgumentException("图片路径无效");
        }

        Path original = uploadRoot.resolve(uploadUrl.substring("/uploads/".length()))
                .toAbsolutePath().normalize();
        if (!original.startsWith(uploadRoot) || !Files.isRegularFile(original)) {
            throw new IllegalArgumentException("图片不存在");
        }
        Path realRoot = uploadRoot.toRealPath();
        if (!original.toRealPath().startsWith(realRoot)) {
            throw new IllegalArgumentException("图片路径无效");
        }

        String fileName = original.getFileName().toString();
        String base = fileName.replaceFirst("\\.[^.]+$", "");
        Path cached = original.resolveSibling(base + "-" + width + ".webp");
        if (Files.isRegularFile(cached)) return cached;

        BufferedImage image = ImageIO.read(original.toFile());
        if (image == null) throw new IllegalArgumentException("无法解析图片内容");
        return variantService.createVariant(image, original, width);
    }
}
