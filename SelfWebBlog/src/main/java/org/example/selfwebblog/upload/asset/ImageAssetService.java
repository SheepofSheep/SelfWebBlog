package org.example.selfwebblog.upload.asset;

import org.example.selfwebblog.service.upload.StoredUpload;
import org.example.selfwebblog.service.upload.UploadStorageService;
import org.example.selfwebblog.service.upload.UploadTarget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class ImageAssetService {
    private final ImageAssetMapper mapper;
    private final UploadStorageService storageService;
    private final ImageVariantService variantService;
    private final Path uploadRoot;

    public ImageAssetService(
            ImageAssetMapper mapper,
            UploadStorageService storageService,
            ImageVariantService variantService,
            @Value("${upload.path}") String uploadPath) {
        this.mapper = mapper;
        this.storageService = storageService;
        this.variantService = variantService;
        this.uploadRoot = Path.of(uploadPath).toAbsolutePath().normalize();
    }

    @Transactional
    public ImageAssetResponse storeArticleImage(MultipartFile file, Long uploaderId) throws IOException {
        if (uploaderId == null) throw new IllegalArgumentException("上传者身份无效");
        byte[] bytes = file.getBytes();
        String hash = sha256(bytes);
        ImageAsset reusable = mapper.findReusable(uploaderId, hash, "ARTICLE");
        if (reusable != null && Files.isRegularFile(pathForUrl(reusable.getOriginalPath()))) {
            return ImageAssetResponse.from(reusable);
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) throw new IllegalArgumentException("无法解析图片内容");

        StoredUpload stored = storageService.storeImage(file, UploadTarget.ARTICLE_IMAGE);
        Path originalFile = pathForUrl(stored.url());
        List<Path> createdFiles = new ArrayList<>();
        createdFiles.add(originalFile);
        try {
            ImageVariantService.VariantPaths variants = variantService.createVariants(image, originalFile);
            if (variants.small() != null) createdFiles.add(variants.small());
            if (variants.large() != null) createdFiles.add(variants.large());

            ImageAsset asset = new ImageAsset();
            asset.setContentHash(hash);
            asset.setPurpose("ARTICLE");
            asset.setOriginalPath(stored.url());
            asset.setSmallWebpPath(urlFor(variants.small()));
            asset.setLargeWebpPath(urlFor(variants.large()));
            asset.setOriginalMime(file.getContentType() == null ? "image/" + stored.extension() : file.getContentType());
            asset.setWidth(image.getWidth());
            asset.setHeight(image.getHeight());
            asset.setByteSize(stored.size());
            asset.setUploaderId(uploaderId);
            asset.setReferenceStatus("ACTIVE");
            asset.setCreateTime(LocalDateTime.now());
            asset.setUpdateTime(LocalDateTime.now());
            mapper.insertAsset(asset);
            ImageAsset saved = mapper.findReusable(uploaderId, hash, "ARTICLE");
            if (saved == null) throw new IllegalStateException("图片资产保存失败");
            return ImageAssetResponse.from(saved);
        } catch (Exception exception) {
            for (Path created : createdFiles) Files.deleteIfExists(created);
            if (exception instanceof IOException ioException) throw ioException;
            if (exception instanceof RuntimeException runtimeException) throw runtimeException;
            throw new IOException("图片处理失败", exception);
        }
    }

    public List<ImageAssetResponse> listArticleAssets(Long uploaderId) {
        if (uploaderId == null) return List.of();
        return mapper.listAssets(uploaderId, "ARTICLE").stream()
                .map(ImageAssetResponse::from)
                .toList();
    }

    private Path pathForUrl(String url) {
        String relative = url.replaceFirst("^/uploads/?", "");
        Path resolved = uploadRoot.resolve(relative).toAbsolutePath().normalize();
        if (!resolved.startsWith(uploadRoot)) throw new IllegalArgumentException("图片路径无效");
        return resolved;
    }

    private String urlFor(Path path) {
        if (path == null) return null;
        String relative = uploadRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
        return "/uploads/" + relative;
    }

    private String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).toLowerCase(Locale.ROOT);
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算图片摘要", exception);
        }
    }
}
