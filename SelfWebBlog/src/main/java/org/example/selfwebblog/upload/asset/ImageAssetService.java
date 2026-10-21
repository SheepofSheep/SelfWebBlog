package org.example.selfwebblog.upload.asset;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import jakarta.annotation.PostConstruct;
import org.example.selfwebblog.service.upload.StoredUpload;
import org.example.selfwebblog.service.upload.UploadStorageService;
import org.example.selfwebblog.service.upload.UploadTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImageAssetService {
    private static final Logger log = LoggerFactory.getLogger(ImageAssetService.class);
    private static final Duration ORPHAN_GRACE_PERIOD = Duration.ofHours(24);
    private static final String DELETION_MANIFEST = "manifest.tsv";
    private static final String DELETION_MANIFEST_VERSION = "IMAGE_ASSET_DELETE_V1";
    private final ImageAssetMapper mapper;
    private final UploadStorageService storageService;
    private final ImageVariantService variantService;
    private final Path uploadRoot;
    private final JdbcTemplate jdbcTemplate;
    private final Object stagingMonitor = new Object();
    private final Set<Path> activeStagingDirectories = ConcurrentHashMap.newKeySet();

    public ImageAssetService(
            ImageAssetMapper mapper,
            UploadStorageService storageService,
            ImageVariantService variantService,
            JdbcTemplate jdbcTemplate,
            @Value("${upload.path}") String uploadPath) {
        this.mapper = mapper;
        this.storageService = storageService;
        this.variantService = variantService;
        this.jdbcTemplate = jdbcTemplate;
        this.uploadRoot = Path.of(uploadPath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void reconcileStagedDeletions() {
        synchronized (stagingMonitor) {
            Path trashRoot = trashRoot();
            if (!Files.isDirectory(trashRoot)) return;
            List<Path> directories;
            try (var entries = Files.list(trashRoot)) {
                directories = entries.toList();
            } catch (IOException exception) {
                log.error("无法读取媒体删除暂存目录，稍后重试", exception);
                return;
            }
            for (Path directory : directories) {
                Path normalized = directory.toAbsolutePath().normalize();
                if (!normalized.startsWith(trashRoot)
                        || !trashRoot.equals(normalized.getParent())) {
                    log.error("忽略越界的媒体删除暂存目录：{}", directory);
                    continue;
                }
                if (activeStagingDirectories.contains(normalized)) continue;
                Path manifest = normalized.resolve(DELETION_MANIFEST);
                if (!Files.isRegularFile(manifest)) {
                    removeEmptyStagingDirectory(normalized);
                    continue;
                }
                try {
                    StagedFiles staged = readDeletionManifest(normalized, manifest);
                    if (mapper.findById(staged.assetId()) == null) staged.commit();
                    else staged.rollback();
                } catch (RuntimeException | IOException exception) {
                    log.error("媒体删除暂存恢复失败，将在下次启动或管理操作时重试：{}", directory, exception);
                }
            }
            removeEmptyStagingDirectory(trashRoot);
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public ImageAssetResponse storeArticleImage(MultipartFile file, Long uploaderId) throws IOException {
        if (uploaderId == null) throw new IllegalArgumentException("上传者身份无效");
        byte[] bytes = file.getBytes();
        String hash = sha256(bytes);
        // SQLite has one writer: serialize the lookup/activation/insert with deletion too.
        mapper.acquireWriteLock();
        ImageAsset reusable = mapper.findReusable(uploaderId, hash, "ARTICLE");
        if (reusable != null && Files.isRegularFile(pathForUrl(reusable.getOriginalPath()))) {
            if (!"ACTIVE".equals(reusable.getReferenceStatus())) {
                if (mapper.updateReferenceStatus(reusable.getId(), "ACTIVE") != 1) {
                    throw new IllegalStateException("图片资产重新激活失败");
                }
                reusable.setReferenceStatus("ACTIVE");
            }
            return ImageAssetResponse.from(reusable);
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) throw new IllegalArgumentException("无法解析图片内容");

        StoredUpload stored = storageService.storeImage(file, UploadTarget.ARTICLE_IMAGE);
        Path originalFile = pathForUrl(stored.url());
        List<Path> createdFiles = new ArrayList<>();
        createdFiles.add(originalFile);
        try {
            if (reusable != null) {
                // Preserve URLs already stored in articles when repairing a missing original.
                Path restored = pathForUrl(reusable.getOriginalPath());
                Files.createDirectories(restored.getParent());
                Files.move(originalFile, restored, StandardCopyOption.REPLACE_EXISTING);
                createdFiles.remove(originalFile);
                originalFile = restored;
                createdFiles.add(originalFile);
            }
            ImageVariantService.VariantPaths variants = variantService.createVariants(image, originalFile);
            if (variants.small() != null) createdFiles.add(variants.small());
            if (variants.large() != null) createdFiles.add(variants.large());

            ImageAsset asset = new ImageAsset();
            asset.setContentHash(hash);
            asset.setPurpose("ARTICLE");
            asset.setOriginalPath(urlFor(originalFile));
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
            if (reusable == null) {
                mapper.insertAsset(asset);
            } else {
                asset.setId(reusable.getId());
                if (mapper.restoreAsset(asset) != 1) throw new IllegalStateException("图片资产修复失败");
            }
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

    @Transactional
    public Page<AdminAssetResponse> listForAdmin(
            int page, int size, String status, String purpose, String keyword) {
        if (page < 1 || size < 1 || size > 100) {
            throw new IllegalArgumentException("分页参数无效");
        }
        String normalizedStatus = filter(status, "资源状态", 30).toUpperCase(Locale.ROOT);
        if (!normalizedStatus.isBlank()
                && !List.of("ACTIVE", "ORPHANED").contains(normalizedStatus)) {
            throw new IllegalArgumentException("资源状态无效");
        }
        String normalizedPurpose = filter(purpose, "资源用途", 30).toUpperCase(Locale.ROOT);
        String normalizedKeyword = filter(keyword, "搜索关键词", 100);
        reconcileStagedDeletions();
        ReferenceReconciliation reconciliation = reconcileReferenceStatuses();
        long total = mapper.countForAdmin(normalizedStatus, normalizedPurpose, normalizedKeyword);
        List<ImageAsset> assets = mapper.listForAdmin(
                normalizedStatus,
                normalizedPurpose,
                normalizedKeyword,
                ((long) page - 1L) * size,
                size);
        List<ReferenceSource> availableSources = reconciliation.sources();
        List<AdminAssetResponse> records = assets.stream()
                .map(asset -> AdminAssetResponse.from(
                        asset,
                        availableSources == null ? null : references(asset, availableSources).size()))
                .toList();
        Page<AdminAssetResponse> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    @Transactional
    public ReferenceScanResult refreshReferenceStatus() {
        return reconcileReferenceStatuses().result();
    }

    private ReferenceReconciliation reconcileReferenceStatuses() {
        List<ImageAsset> assets = mapper.listAllAssets();
        List<ReferenceSource> sources;
        try {
            sources = referenceSources();
        } catch (RuntimeException exception) {
            log.warn("无法完整读取媒体引用，所有资源暂时保持 ACTIVE", exception);
            for (ImageAsset asset : assets) mapper.updateReferenceStatus(asset.getId(), "ACTIVE");
            return new ReferenceReconciliation(
                    new ReferenceScanResult(assets.size(), 0), null);
        }
        int active = 0;
        int orphaned = 0;
        LocalDateTime orphanCutoff = LocalDateTime.now().minus(ORPHAN_GRACE_PERIOD);
        for (ImageAsset asset : assets) {
            boolean referenced = !references(asset, sources).isEmpty();
            boolean withinGracePeriod = asset.getCreateTime() != null
                    && asset.getCreateTime().isAfter(orphanCutoff);
            boolean activeAsset = referenced || withinGracePeriod;
            String status = activeAsset ? "ACTIVE" : "ORPHANED";
            mapper.updateReferenceStatus(asset.getId(), status);
            if (activeAsset) active++; else orphaned++;
        }
        return new ReferenceReconciliation(
                new ReferenceScanResult(active, orphaned), List.copyOf(sources));
    }

    @Transactional
    public AssetReferenceDetails referencesForAdmin(Long id) {
        ImageAsset asset = mapper.findById(id);
        if (asset == null) throw new IllegalArgumentException("图片资产不存在");
        List<AssetReference> references = references(asset, referenceSources());
        boolean withinGracePeriod = asset.getCreateTime() != null
                && asset.getCreateTime().isAfter(LocalDateTime.now().minus(ORPHAN_GRACE_PERIOD));
        String currentStatus = !references.isEmpty() || withinGracePeriod ? "ACTIVE" : "ORPHANED";
        if (!currentStatus.equals(asset.getReferenceStatus())) {
            mapper.updateReferenceStatus(asset.getId(), currentStatus);
        }
        return new AssetReferenceDetails(
                asset.getId(), currentStatus, references.size(), references);
    }

    @Transactional(
            rollbackFor = IOException.class,
            noRollbackFor = AssetDeletionRefusedException.class)
    public void deleteOrphan(Long id) throws IOException {
        deleteOrphansInternal(Collections.singletonList(id));
    }

    @Transactional(
            rollbackFor = IOException.class,
            noRollbackFor = AssetDeletionRefusedException.class)
    public int deleteOrphans(List<Long> ids) throws IOException {
        return deleteOrphansInternal(ids);
    }

    private int deleteOrphansInternal(List<Long> ids) throws IOException {
        List<Long> normalizedIds = normalizeDeletionIds(ids);
        mapper.acquireWriteLock();
        reconcileStagedDeletions();
        List<ImageAsset> assets = normalizedIds.stream().map(id -> {
            ImageAsset asset = mapper.findById(id);
            if (asset == null) throw new IllegalArgumentException("图片资产不存在");
            if (!"ORPHANED".equals(asset.getReferenceStatus())) {
                throw new IllegalArgumentException("仅能删除已确认未引用的图片");
            }
            return asset;
        }).toList();

        List<ReferenceSource> sources;
        try {
            sources = referenceSources();
        } catch (RuntimeException exception) {
            for (ImageAsset asset : assets) {
                mapper.updateReferenceStatus(asset.getId(), "ACTIVE");
            }
            throw new AssetDeletionRefusedException("无法确认图片引用状态，已停止删除", exception);
        }
        for (ImageAsset asset : assets) {
            if (!references(asset, sources).isEmpty()) {
                mapper.updateReferenceStatus(asset.getId(), "ACTIVE");
                throw new AssetDeletionRefusedException("图片仍被内容引用，已停止删除");
            }
        }

        List<StagedFiles> staged = new ArrayList<>();
        try {
            for (ImageAsset asset : assets) staged.add(stageFiles(asset));
        } catch (RuntimeException | IOException exception) {
            rollbackStaged(staged, exception);
            throw exception;
        }
        boolean synchronizedTransaction = TransactionSynchronizationManager.isSynchronizationActive();
        boolean cleanupRegistered = false;
        try {
            if (synchronizedTransaction) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        try {
                            if (status == STATUS_COMMITTED) commitStaged(staged);
                            else rollbackStaged(staged, null);
                        } catch (RuntimeException | IOException exception) {
                            log.error("媒体批量删除暂存目录清理失败，将在下次管理操作时重试", exception);
                        }
                    }
                });
                cleanupRegistered = true;
            }
            for (ImageAsset asset : assets) {
                if (mapper.deleteOrphan(asset.getId()) != 1) {
                    throw new IllegalStateException("图片资产删除失败");
                }
            }
            if (!cleanupRegistered) commitStaged(staged);
        } catch (RuntimeException | IOException exception) {
            if (!cleanupRegistered) reconcileUnsynchronizedStaging(staged, exception);
            throw exception;
        }
        return assets.size();
    }

    private void reconcileUnsynchronizedStaging(
            List<StagedFiles> staged, Throwable cause) {
        for (StagedFiles files : staged) {
            activeStagingDirectories.remove(files.directory());
        }
        try {
            reconcileStagedDeletions();
        } catch (RuntimeException exception) {
            cause.addSuppressed(exception);
        }
    }

    private List<Long> normalizeDeletionIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("请选择要删除的图片");
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null || id <= 0) throw new IllegalArgumentException("图片编号无效");
            unique.add(id);
        }
        if (unique.size() > 100) throw new IllegalArgumentException("单次最多删除 100 个图片");
        return List.copyOf(unique);
    }

    private void commitStaged(List<StagedFiles> staged) throws IOException {
        IOException failure = null;
        for (StagedFiles files : staged) {
            synchronized (stagingMonitor) {
                try {
                    files.commit();
                } catch (IOException exception) {
                    if (failure == null) failure = exception;
                    else failure.addSuppressed(exception);
                } finally {
                    activeStagingDirectories.remove(files.directory());
                }
            }
        }
        if (failure != null) throw failure;
    }

    private void rollbackStaged(List<StagedFiles> staged, Throwable cause) throws IOException {
        IOException failure = null;
        for (int index = staged.size() - 1; index >= 0; index--) {
            StagedFiles files = staged.get(index);
            synchronized (stagingMonitor) {
                try {
                    files.rollback();
                } catch (IOException exception) {
                    if (failure == null) failure = exception;
                    else failure.addSuppressed(exception);
                } finally {
                    activeStagingDirectories.remove(files.directory());
                }
            }
        }
        if (failure != null) {
            if (cause != null) cause.addSuppressed(failure);
            else throw failure;
        }
    }

    private List<ReferenceSource> referenceSources() {
        List<ReferenceSource> sources = new ArrayList<>();
        jdbcTemplate.query("""
                SELECT id, title, content, cover_url, image_url, background_value FROM post
                """, result -> {
            String targetId = String.valueOf(result.getLong("id"));
            String label = value(result.getString("title"));
            addSource(sources, "POST", targetId, label, "content", result.getString("content"));
            addSource(sources, "POST", targetId, label, "coverUrl", result.getString("cover_url"));
            addSource(sources, "POST", targetId, label, "imageUrl", result.getString("image_url"));
            addSource(sources, "POST", targetId, label, "backgroundValue", result.getString("background_value"));
        });
        jdbcTemplate.query("SELECT id, avatar_url, bg_url FROM blog_info", result -> {
            String targetId = String.valueOf(result.getLong("id"));
            addSource(sources, "SITE", targetId, "站点", "avatarUrl", result.getString("avatar_url"));
            addSource(sources, "SITE", targetId, "站点", "bgUrl", result.getString("bg_url"));
        });
        return sources;
    }

    private void addSource(List<ReferenceSource> sources, String sourceType, String targetId,
                           String label, String field, String content) {
        if (content != null && !content.isBlank()) {
            sources.add(new ReferenceSource(sourceType, targetId, label, field, content));
        }
    }

    private List<AssetReference> references(ImageAsset asset, List<ReferenceSource> sources) {
        List<String> paths = List.of(
                        asset.getOriginalPath(),
                        value(asset.getSmallWebpPath()),
                        value(asset.getLargeWebpPath())).stream()
                .filter(path -> path != null && !path.isBlank())
                .distinct()
                .toList();
        return sources.stream()
                .filter(source -> paths.stream().anyMatch(source.content()::contains))
                .map(source -> new AssetReference(
                        source.sourceType(), source.targetId(), source.label(), source.field()))
                .toList();
    }

    private Path trashRoot() {
        Path trashRoot = uploadRoot.resolve(".asset-trash").toAbsolutePath().normalize();
        if (!trashRoot.startsWith(uploadRoot) || trashRoot.equals(uploadRoot)) {
            throw new IllegalArgumentException("媒体暂存目录无效");
        }
        return trashRoot;
    }

    private StagedFiles stageFiles(ImageAsset asset) throws IOException {
        Path trashRoot = trashRoot();
        Path directory = trashRoot.resolve(UUID.randomUUID().toString()).toAbsolutePath().normalize();
        if (!directory.startsWith(trashRoot)) throw new IllegalArgumentException("媒体暂存目录无效");
        List<StagedFile> files = new ArrayList<>();
        Path manifest = directory.resolve(DELETION_MANIFEST);
        try {
            synchronized (stagingMonitor) {
                Files.createDirectories(directory);
                activeStagingDirectories.add(directory);
            }
            List<String> urls = List.of(
                            asset.getOriginalPath(),
                            value(asset.getSmallWebpPath()),
                            value(asset.getLargeWebpPath())).stream()
                    .filter(url -> url != null && !url.isBlank())
                    .distinct()
                    .toList();
            for (int index = 0; index < urls.size(); index++) {
                Path source = pathForUrl(urls.get(index));
                if (!Files.isRegularFile(source)) continue;
                String fileName = source.getFileName() == null ? "asset" : source.getFileName().toString();
                Path staged = directory.resolve(index + "-" + fileName).normalize();
                files.add(new StagedFile(source, staged));
            }
            StagedFiles staged = new StagedFiles(asset.getId(), directory, manifest, List.copyOf(files));
            writeDeletionManifest(staged);
            for (StagedFile file : files) {
                Files.move(file.original(), file.staged(), StandardCopyOption.REPLACE_EXISTING);
            }
            return staged;
        } catch (RuntimeException | IOException exception) {
            StagedFiles partial = new StagedFiles(asset.getId(), directory, manifest, List.copyOf(files));
            synchronized (stagingMonitor) {
                try {
                    partial.rollback();
                } catch (IOException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                } finally {
                    activeStagingDirectories.remove(directory);
                }
            }
            throw exception;
        }
    }

    private void writeDeletionManifest(StagedFiles staged) throws IOException {
        if (staged.assetId() == null || staged.assetId() <= 0) {
            throw new IllegalArgumentException("图片编号无效");
        }
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        StringBuilder content = new StringBuilder()
                .append(DELETION_MANIFEST_VERSION)
                .append('\t')
                .append(staged.assetId())
                .append('\n');
        for (StagedFile file : staged.files()) {
            content.append(encodePath(file.original(), encoder))
                    .append('\t')
                    .append(encodePath(file.staged(), encoder))
                    .append('\n');
        }
        Files.writeString(staged.manifest(), content, StandardCharsets.UTF_8);
    }

    private String encodePath(Path path, Base64.Encoder encoder) {
        return encoder.encodeToString(
                path.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
    }

    private StagedFiles readDeletionManifest(Path directory, Path manifest) throws IOException {
        List<String> lines = Files.readAllLines(manifest, StandardCharsets.UTF_8);
        if (lines.isEmpty()) throw new IllegalArgumentException("媒体删除清单为空");
        String[] header = lines.get(0).split("\\t", -1);
        if (header.length != 2 || !DELETION_MANIFEST_VERSION.equals(header[0])) {
            throw new IllegalArgumentException("媒体删除清单版本无效");
        }
        Long assetId;
        try {
            assetId = Long.valueOf(header[1]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("媒体删除清单编号无效", exception);
        }
        if (assetId <= 0) throw new IllegalArgumentException("媒体删除清单编号无效");
        List<StagedFile> files = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String[] paths = lines.get(index).split("\\t", -1);
            if (paths.length != 2) throw new IllegalArgumentException("媒体删除清单内容无效");
            Path original = decodePath(paths[0]);
            Path staged = decodePath(paths[1]);
            validateManifestPaths(directory, original, staged);
            files.add(new StagedFile(original, staged));
        }
        return new StagedFiles(assetId, directory, manifest, List.copyOf(files));
    }

    private Path decodePath(String encoded) {
        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            return Path.of(decoded).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("媒体删除清单路径无效", exception);
        }
    }

    private void validateManifestPaths(Path directory, Path original, Path staged) {
        Path trashRoot = trashRoot();
        if (!original.startsWith(uploadRoot)
                || original.equals(uploadRoot)
                || original.startsWith(trashRoot)) {
            throw new IllegalArgumentException("媒体删除清单原始路径越界");
        }
        if (!staged.startsWith(directory)
                || staged.equals(directory)
                || !directory.equals(staged.getParent())) {
            throw new IllegalArgumentException("媒体删除清单暂存路径越界");
        }
    }

    private void removeEmptyStagingDirectory(Path directory) {
        if (!Files.isDirectory(directory)) return;
        try (var entries = Files.list(directory)) {
            if (entries.findAny().isEmpty()) Files.deleteIfExists(directory);
        } catch (IOException exception) {
            log.debug("媒体空暂存目录将在下次协调时清理：{}", directory, exception);
        }
    }

    private Path pathForUrl(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            throw new IllegalArgumentException("图片路径无效");
        }
        String relative = url.replaceFirst("^/uploads/?", "");
        Path resolved = uploadRoot.resolve(relative).toAbsolutePath().normalize();
        if (!resolved.startsWith(uploadRoot)
                || resolved.equals(uploadRoot)
                || resolved.startsWith(trashRoot())) {
            throw new IllegalArgumentException("图片路径无效");
        }
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

    private String filter(String value, String label, int maximum) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maximum) throw new IllegalArgumentException(label + "内容过长");
        return normalized;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    public record AdminAssetResponse(
            @JsonSerialize(using = ToStringSerializer.class) Long id,
            String originalUrl,
            String smallWebpUrl,
            String largeWebpUrl,
            int width,
            int height,
            long byteSize,
            String purpose,
            String referenceStatus,
            @JsonSerialize(using = ToStringSerializer.class) Long uploaderId,
            Integer referenceCount,
            LocalDateTime createTime) {
        static AdminAssetResponse from(ImageAsset asset, Integer referenceCount) {
            return new AdminAssetResponse(
                    asset.getId(), asset.getOriginalPath(), asset.getSmallWebpPath(),
                    asset.getLargeWebpPath(), asset.getWidth(), asset.getHeight(),
                    asset.getByteSize(), asset.getPurpose(), asset.getReferenceStatus(),
                    asset.getUploaderId(), referenceCount, asset.getCreateTime());
        }
    }

    public record ReferenceScanResult(int active, int orphaned) {
    }

    public record AssetReference(String sourceType, String targetId, String label, String field) {
    }

    public record AssetReferenceDetails(
            @JsonSerialize(using = ToStringSerializer.class) Long assetId,
            String referenceStatus,
            int referenceCount,
            List<AssetReference> references) {
    }

    public static final class AssetDeletionRefusedException extends IllegalArgumentException {
        public AssetDeletionRefusedException(String message) {
            super(message);
        }

        public AssetDeletionRefusedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record ReferenceSource(
            String sourceType, String targetId, String label, String field, String content) {
    }

    private record ReferenceReconciliation(
            ReferenceScanResult result, List<ReferenceSource> sources) {
    }

    private record StagedFile(Path original, Path staged) {
    }

    private record StagedFiles(
            Long assetId, Path directory, Path manifest, List<StagedFile> files) {
        void commit() throws IOException {
            for (StagedFile file : files) Files.deleteIfExists(file.staged());
            Files.deleteIfExists(manifest);
            Files.deleteIfExists(directory);
            Path parent = directory.getParent();
            if (parent != null) {
                try (var entries = Files.list(parent)) {
                    if (entries.findAny().isEmpty()) Files.deleteIfExists(parent);
                }
            }
        }

        void rollback() throws IOException {
            IOException failure = null;
            for (int index = files.size() - 1; index >= 0; index--) {
                StagedFile file = files.get(index);
                try {
                    Files.createDirectories(file.original().getParent());
                    if (Files.exists(file.staged())) {
                        Files.move(file.staged(), file.original(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException exception) {
                    if (failure == null) failure = exception;
                    else failure.addSuppressed(exception);
                }
            }
            if (failure == null) {
                try {
                    Files.deleteIfExists(manifest);
                    Files.deleteIfExists(directory);
                    Path parent = directory.getParent();
                    if (parent != null) {
                        try (var entries = Files.list(parent)) {
                            if (entries.findAny().isEmpty()) Files.deleteIfExists(parent);
                        }
                    }
                } catch (IOException exception) {
                    failure = exception;
                }
            }
            if (failure != null) throw failure;
        }
    }
}
