package org.example.selfwebblog.upload.asset;

import org.example.selfwebblog.service.upload.UploadStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageAssetServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void paginatesAndNormalizesAdminAssetFilters() {
        ImageAsset asset = asset(12L, "/uploads/cover.png", "ORPHANED");
        asset.setSmallWebpPath("/uploads/cover-small.webp");
        asset.setLargeWebpPath("/uploads/cover-large.webp");
        asset.setWidth(1200);
        asset.setHeight(800);
        asset.setByteSize(2000L);
        asset.setCreateTime(LocalDateTime.of(2026, 7, 26, 10, 0));
        ImageAssetMapper mapper = mock(ImageAssetMapper.class);
        when(mapper.countForAdmin("ORPHANED", "ARTICLE", "cover")).thenReturn(25L);
        when(mapper.listForAdmin("ORPHANED", "ARTICLE", "cover", 10, 10))
                .thenReturn(List.of(asset));
        ImageAssetService service = service(mapper, mock(JdbcTemplate.class));

        var page = service.listForAdmin(2, 10, " orphaned ", " article ", " cover ");

        assertEquals(2, page.getCurrent());
        assertEquals(10, page.getSize());
        assertEquals(25, page.getTotal());
        assertEquals(12L, page.getRecords().get(0).id());
        verify(mapper).listForAdmin("ORPHANED", "ARTICLE", "cover", 10, 10);
    }

    @Test
    void adminListRepairsReferencesBeforeApplyingTheActiveStatusFilter() {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update("""
                    INSERT INTO post(id, title, content, cover_url, image_url, background_value)
                    VALUES (12, '重新引用', '', '/uploads/reused.png', '', '')
                    """);
            ImageAsset stale = asset(15L, "/uploads/reused.png", "ORPHANED");
            stale.setCreateTime(LocalDateTime.now().minusDays(2));
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.listAllAssets()).thenReturn(List.of(stale));
            doAnswer(invocation -> {
                stale.setReferenceStatus(invocation.getArgument(1, String.class));
                return 1;
            }).when(mapper).updateReferenceStatus(15L, "ACTIVE");
            when(mapper.countForAdmin("ACTIVE", "", "")).thenReturn(1L);
            when(mapper.listForAdmin("ACTIVE", "", "", 0, 20)).thenReturn(List.of(stale));
            ImageAssetService service = service(mapper, jdbc);

            var page = service.listForAdmin(1, 20, "ACTIVE", null, null);

            assertEquals(1, page.getTotal());
            assertEquals("ACTIVE", page.getRecords().get(0).referenceStatus());
            assertEquals(1, page.getRecords().get(0).referenceCount());
            var order = inOrder(mapper);
            order.verify(mapper).updateReferenceStatus(15L, "ACTIVE");
            order.verify(mapper).countForAdmin("ACTIVE", "", "");
            order.verify(mapper).listForAdmin("ACTIVE", "", "", 0, 20);
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void rejectsUnsupportedAdminAssetFiltersAndPagination() {
        ImageAssetMapper mapper = mock(ImageAssetMapper.class);
        ImageAssetService service = service(mapper, mock(JdbcTemplate.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.listForAdmin(0, 10, null, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.listForAdmin(1, 10, "DELETED", null, null));

        verifyNoInteractions(mapper);
    }

    @Test
    void keepsLargeAdminPageOffsetsFromOverflowing() {
        ImageAssetMapper mapper = mock(ImageAssetMapper.class);
        when(mapper.countForAdmin("", "", "")).thenReturn(1L);
        when(mapper.listForAdmin("", "", "", 214748364600L, 100)).thenReturn(List.of());
        ImageAssetService service = service(mapper, mock(JdbcTemplate.class));

        service.listForAdmin(Integer.MAX_VALUE, 100, null, null, null);

        verify(mapper).listForAdmin("", "", "", 214748364600L, 100);
    }

    @Test
    void reusesExistingAssetForIdenticalContent() throws Exception {
        byte[] content = "same-image-content".getBytes();
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        Files.write(tempDir.resolve("existing.png"), content);

        ImageAsset asset = new ImageAsset();
        asset.setId(7L);
        asset.setContentHash(hash);
        asset.setPurpose("ARTICLE");
        asset.setOriginalPath("/uploads/existing.png");
        asset.setWidth(10);
        asset.setHeight(10);

        ImageAssetMapper mapper = mock(ImageAssetMapper.class);
        UploadStorageService storage = mock(UploadStorageService.class);
        ImageVariantService variants = mock(ImageVariantService.class);
        when(mapper.findReusable(3L, hash, "ARTICLE")).thenReturn(asset);
        ImageAssetService service = new ImageAssetService(
                mapper, storage, variants, mock(JdbcTemplate.class), tempDir.toString());

        ImageAssetResponse response = service.storeArticleImage(
                new MockMultipartFile("file", "a.png", "image/png", content), 3L);

        assertEquals(7L, response.id());
        verifyNoInteractions(storage, variants);
    }

    @Test
    void scansEverySupportedPostAndSiteReference() {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update("""
                    INSERT INTO post(id, title, content, cover_url, image_url, background_value)
                    VALUES
                      (1, '正文图', '![](/uploads/content.png)', '', '', ''),
                      (2, '封面图', '', '/uploads/cover.png', '', ''),
                      (3, '旧图', '', '', '/uploads/legacy.png', ''),
                      (4, '背景图', '', '', '', '/uploads/background.png')
                    """);
            jdbc.update("""
                    INSERT INTO blog_info(id, avatar_url, bg_url)
                    VALUES (1, '/uploads/avatar.png', '/uploads/site-bg.png')
                    """);

            List<ImageAsset> assets = List.of(
                    asset(1L, "/uploads/content.png", "ACTIVE"),
                    asset(2L, "/uploads/cover.png", "ACTIVE"),
                    asset(3L, "/uploads/legacy.png", "ACTIVE"),
                    asset(4L, "/uploads/background.png", "ACTIVE"),
                    asset(5L, "/uploads/avatar.png", "ACTIVE"),
                    asset(6L, "/uploads/site-bg.png", "ACTIVE"),
                    asset(7L, "/uploads/orphan.png", "ACTIVE"));
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.listAllAssets()).thenReturn(assets);
            ImageAssetService service = service(mapper, jdbc);

            ImageAssetService.ReferenceScanResult result = service.refreshReferenceStatus();

            assertEquals(6, result.active());
            assertEquals(1, result.orphaned());
            for (long id = 1; id <= 6; id++) verify(mapper).updateReferenceStatus(id, "ACTIVE");
            verify(mapper).updateReferenceStatus(7L, "ORPHANED");
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void keepsRecentlyUploadedUnreferencedAssetsActiveDuringTheGracePeriod() {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            ImageAsset recent = asset(13L, "/uploads/recent.png", "ACTIVE");
            recent.setCreateTime(LocalDateTime.now().minusHours(23));
            ImageAsset expired = asset(14L, "/uploads/expired.png", "ACTIVE");
            expired.setCreateTime(LocalDateTime.now().minusHours(25));
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.listAllAssets()).thenReturn(List.of(recent, expired));
            ImageAssetService service = service(mapper, new JdbcTemplate(dataSource));

            ImageAssetService.ReferenceScanResult result = service.refreshReferenceStatus();

            assertEquals(1, result.active());
            assertEquals(1, result.orphaned());
            verify(mapper).updateReferenceStatus(13L, "ACTIVE");
            verify(mapper).updateReferenceStatus(14L, "ORPHANED");
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void returnsReferenceDetailsForOriginalAndOptimizedVariants() {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update("""
                    INSERT INTO post(id, title, content, cover_url, image_url, background_value)
                    VALUES (9, '文章', '![](/uploads/a-small.webp)', '/uploads/a.png', '', '')
                    """);
            ImageAsset asset = asset(8L, "/uploads/a.png", "ACTIVE");
            asset.setSmallWebpPath("/uploads/a-small.webp");
            asset.setLargeWebpPath("/uploads/a-large.webp");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(8L)).thenReturn(asset);
            ImageAssetService service = service(mapper, jdbc);

            ImageAssetService.AssetReferenceDetails details = service.referencesForAdmin(8L);

            assertEquals(2, details.referenceCount());
            assertEquals(List.of("content", "coverUrl"),
                    details.references().stream().map(ImageAssetService.AssetReference::field).toList());
            assertEquals("9", details.references().get(0).targetId());
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void referenceDetailsRepairAStaleOrphanStatusBeforeReturningIt() {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update("""
                    INSERT INTO post(id, title, content, cover_url, image_url, background_value)
                    VALUES (10, '文章', '', '/uploads/referenced.png', '', '')
                    """);
            ImageAsset stale = asset(9L, "/uploads/referenced.png", "ORPHANED");
            stale.setCreateTime(LocalDateTime.now().minusDays(3));
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(9L)).thenReturn(stale);
            ImageAssetService service = service(mapper, jdbc);

            ImageAssetService.AssetReferenceDetails details = service.referencesForAdmin(9L);

            assertEquals("ACTIVE", details.referenceStatus());
            assertEquals(1, details.referenceCount());
            verify(mapper).updateReferenceStatus(9L, "ACTIVE");
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void unknownReferenceStateFailsSafeToActive() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        try {
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.listAllAssets()).thenReturn(List.of(asset(1L, "/uploads/a.png", "ACTIVE")));
            ImageAssetService service = service(mapper, new JdbcTemplate(dataSource));

            ImageAssetService.ReferenceScanResult result = service.refreshReferenceStatus();

            assertEquals(1, result.active());
            assertEquals(0, result.orphaned());
            verify(mapper).updateReferenceStatus(1L, "ACTIVE");
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void restoresStagedFilesWhenTheDatabaseDeleteFails() throws Exception {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            Path original = tempDir.resolve("orphan.png");
            Files.writeString(original, "image");
            ImageAsset orphan = asset(3L, "/uploads/orphan.png", "ORPHANED");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(3L)).thenReturn(orphan);
            when(mapper.deleteOrphan(3L)).thenReturn(0);
            ImageAssetService service = service(mapper, new JdbcTemplate(dataSource));

            assertThrows(IllegalStateException.class, () -> service.deleteOrphan(3L));

            assertTrue(Files.exists(original));
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void persistsADeletionManifestBeforeRemovingTheDatabaseRow() throws Exception {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            Path original = tempDir.resolve("manifested.png");
            Files.writeString(original, "image");
            ImageAsset orphan = asset(18L, "/uploads/manifested.png", "ORPHANED");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(18L)).thenReturn(orphan);
            when(mapper.deleteOrphan(18L)).thenAnswer(invocation -> {
                Path trashRoot = tempDir.resolve(".asset-trash");
                try (var directories = Files.list(trashRoot)) {
                    assertTrue(directories.anyMatch(directory ->
                            Files.isRegularFile(directory.resolve("manifest.tsv"))));
                }
                return 1;
            });
            ImageAssetService service = service(mapper, new JdbcTemplate(dataSource));

            service.deleteOrphan(18L);

            assertTrue(Files.notExists(original));
            assertTrue(Files.notExists(tempDir.resolve(".asset-trash")));
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void restoresPersistedStagingWhenTheDatabaseRowStillExists() throws Exception {
        Path original = tempDir.resolve("recover-existing.png");
        Path directory = tempDir.resolve(".asset-trash/recover-existing");
        Path staged = directory.resolve("0-recover-existing.png");
        Files.createDirectories(directory);
        Files.writeString(staged, "recover me");
        writeDeletionManifest(directory, 19L, original, staged);
        ImageAssetMapper mapper = mock(ImageAssetMapper.class);
        when(mapper.findById(19L)).thenReturn(asset(19L, "/uploads/recover-existing.png", "ORPHANED"));
        ImageAssetService service = service(mapper, mock(JdbcTemplate.class));

        service.reconcileStagedDeletions();

        assertEquals("recover me", Files.readString(original));
        assertTrue(Files.notExists(directory));
        assertTrue(Files.notExists(tempDir.resolve(".asset-trash")));
    }

    @Test
    void finishesPersistedStagingWhenTheDatabaseRowWasDeleted() throws Exception {
        Path original = tempDir.resolve("recover-deleted.png");
        Path directory = tempDir.resolve(".asset-trash/recover-deleted");
        Path staged = directory.resolve("0-recover-deleted.png");
        Files.createDirectories(directory);
        Files.writeString(staged, "delete me");
        writeDeletionManifest(directory, 20L, original, staged);
        ImageAssetMapper mapper = mock(ImageAssetMapper.class);
        ImageAssetService service = service(mapper, mock(JdbcTemplate.class));

        service.reconcileStagedDeletions();

        assertTrue(Files.notExists(original));
        assertTrue(Files.notExists(directory));
        assertTrue(Files.notExists(tempDir.resolve(".asset-trash")));
    }

    @Test
    void rejectsPersistedStagingPathsOutsideTheUploadRoot() throws Exception {
        Path outside = tempDir.resolveSibling("outside-" + UUID.randomUUID() + ".png");
        Path directory = tempDir.resolve(".asset-trash/recover-invalid");
        Path staged = directory.resolve("0-recover-invalid.png");
        Files.createDirectories(directory);
        Files.writeString(staged, "do not move outside");
        writeDeletionManifest(directory, 21L, outside, staged);
        ImageAssetMapper mapper = mock(ImageAssetMapper.class);
        when(mapper.findById(21L)).thenReturn(asset(21L, "/uploads/ignored.png", "ORPHANED"));
        ImageAssetService service = service(mapper, mock(JdbcTemplate.class));

        service.reconcileStagedDeletions();

        assertTrue(Files.notExists(outside));
        assertTrue(Files.exists(staged));
        assertTrue(Files.exists(directory.resolve("manifest.tsv")));
    }

    @Test
    void restoresTheOriginalWhenASecondVariantPathIsInvalid() throws Exception {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            Path original = tempDir.resolve("variant-original.png");
            Files.writeString(original, "image");
            ImageAsset orphan = asset(15L, "/uploads/variant-original.png", "ORPHANED");
            orphan.setSmallWebpPath("https://example.invalid/variant.webp");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(15L)).thenReturn(orphan);
            ImageAssetService service = service(mapper, new JdbcTemplate(dataSource));

            assertThrows(IllegalArgumentException.class, () -> service.deleteOrphan(15L));

            assertTrue(Files.exists(original));
            verify(mapper, never()).deleteOrphan(15L);
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void refusesToDeleteAnAssetThatHasBecomeReferenced() throws Exception {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.update("""
                    INSERT INTO post(id, title, content, cover_url, image_url, background_value)
                    VALUES (4, '文章', '![](/uploads/in-use.png)', '', '', '')
                    """);
            ImageAsset referenced = asset(4L, "/uploads/in-use.png", "ORPHANED");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(4L)).thenReturn(referenced);
            ImageAssetService service = service(mapper, jdbc);

            assertThrows(IllegalArgumentException.class, () -> service.deleteOrphan(4L));

            verify(mapper).updateReferenceStatus(4L, "ACTIVE");
            verify(mapper, never()).deleteOrphan(4L);
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void persistsTheFailSafeActiveStatusWhenTransactionalDeletionIsRefused() throws Exception {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE asset_status (id INTEGER PRIMARY KEY, status TEXT NOT NULL)");
            jdbc.update("INSERT INTO asset_status(id, status) VALUES (5, 'ORPHANED')");
            jdbc.update("""
                    INSERT INTO post(id, title, content, cover_url, image_url, background_value)
                    VALUES (5, '文章', '', '/uploads/referenced.png', '', '')
                    """);
            ImageAsset referenced = asset(5L, "/uploads/referenced.png", "ORPHANED");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(5L)).thenReturn(referenced);
            doAnswer(invocation -> {
                String status = invocation.getArgument(1, String.class);
                Long id = invocation.getArgument(0, Long.class);
                return jdbc.update(
                        "UPDATE asset_status SET status = ? WHERE id = ?", status, id);
            })
                    .when(mapper).updateReferenceStatus(5L, "ACTIVE");
            ImageAssetService target = service(mapper, jdbc);
            ImageAssetService transactional = transactional(target, dataSource);

            assertThrows(IllegalArgumentException.class, () -> transactional.deleteOrphan(5L));

            assertEquals("ACTIVE", jdbc.queryForObject(
                    "SELECT status FROM asset_status WHERE id = 5", String.class));
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void restoresFilesAndDatabaseRowAfterTransactionalDeleteFailure() throws Exception {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE asset_status (id INTEGER PRIMARY KEY, status TEXT NOT NULL)");
            jdbc.update("INSERT INTO asset_status(id, status) VALUES (6, 'ORPHANED')");
            Path original = tempDir.resolve("rollback.png");
            Files.writeString(original, "image");
            ImageAsset orphan = asset(6L, "/uploads/rollback.png", "ORPHANED");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(6L)).thenReturn(orphan);
            when(mapper.deleteOrphan(6L)).thenAnswer(invocation -> {
                jdbc.update("DELETE FROM asset_status WHERE id = 6");
                throw new IllegalStateException("database failure");
            });
            ImageAssetService transactional = transactional(service(mapper, jdbc), dataSource);

            assertThrows(IllegalStateException.class, () -> transactional.deleteOrphan(6L));

            assertTrue(Files.exists(original));
            assertEquals(1L, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM asset_status WHERE id = 6", Long.class));
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void batchDeletionValidatesEveryAssetBeforeTouchingFilesOrRows() throws Exception {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            Path orphanFile = tempDir.resolve("batch-orphan.png");
            Files.writeString(orphanFile, "image");
            ImageAsset orphan = asset(7L, "/uploads/batch-orphan.png", "ORPHANED");
            ImageAsset active = asset(8L, "/uploads/active.png", "ACTIVE");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(7L)).thenReturn(orphan);
            when(mapper.findById(8L)).thenReturn(active);
            ImageAssetService service = service(mapper, new JdbcTemplate(dataSource));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.deleteOrphans(List.of(7L, 8L)));

            assertTrue(Files.exists(orphanFile));
            verify(mapper, never()).deleteOrphan(7L);
            verify(mapper, never()).deleteOrphan(8L);
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void batchStagingRestoresEarlierAssetsWhenALaterAssetPathIsInvalid() throws Exception {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            Path firstFile = tempDir.resolve("valid-first.png");
            Files.writeString(firstFile, "first");
            ImageAsset first = asset(16L, "/uploads/valid-first.png", "ORPHANED");
            ImageAsset invalidSecond = asset(17L, "https://example.invalid/second.png", "ORPHANED");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(16L)).thenReturn(first);
            when(mapper.findById(17L)).thenReturn(invalidSecond);
            ImageAssetService service = service(mapper, new JdbcTemplate(dataSource));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> service.deleteOrphans(List.of(16L, 17L)));

            assertTrue(Files.exists(firstFile));
            verify(mapper, never()).deleteOrphan(16L);
            verify(mapper, never()).deleteOrphan(17L);
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void batchDeletionRemovesDistinctConfirmedOrphans() throws Exception {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            Path firstFile = tempDir.resolve("first-orphan.png");
            Path secondFile = tempDir.resolve("second-orphan.png");
            Files.writeString(firstFile, "first");
            Files.writeString(secondFile, "second");
            ImageAsset first = asset(9L, "/uploads/first-orphan.png", "ORPHANED");
            ImageAsset second = asset(10L, "/uploads/second-orphan.png", "ORPHANED");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(9L)).thenReturn(first);
            when(mapper.findById(10L)).thenReturn(second);
            when(mapper.deleteOrphan(9L)).thenReturn(1);
            when(mapper.deleteOrphan(10L)).thenReturn(1);
            ImageAssetService service = service(mapper, new JdbcTemplate(dataSource));

            int deleted = service.deleteOrphans(List.of(9L, 10L, 9L));

            assertEquals(2, deleted);
            verify(mapper).deleteOrphan(9L);
            verify(mapper).deleteOrphan(10L);
            assertTrue(Files.notExists(firstFile));
            assertTrue(Files.notExists(secondFile));
        } finally {
            dataSource.destroy();
        }
    }

    @Test
    void batchDeletionRestoresEveryFileAndRowWhenOneDeleteFails() throws Exception {
        SingleConnectionDataSource dataSource = referenceDataSource();
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("CREATE TABLE asset_status (id INTEGER PRIMARY KEY, status TEXT NOT NULL)");
            jdbc.update("INSERT INTO asset_status(id, status) VALUES (11, 'ORPHANED'), (12, 'ORPHANED')");
            Path firstFile = tempDir.resolve("rollback-first.png");
            Path secondFile = tempDir.resolve("rollback-second.png");
            Files.writeString(firstFile, "first");
            Files.writeString(secondFile, "second");
            ImageAsset first = asset(11L, "/uploads/rollback-first.png", "ORPHANED");
            ImageAsset second = asset(12L, "/uploads/rollback-second.png", "ORPHANED");
            ImageAssetMapper mapper = mock(ImageAssetMapper.class);
            when(mapper.findById(11L)).thenReturn(first);
            when(mapper.findById(12L)).thenReturn(second);
            when(mapper.deleteOrphan(11L)).thenAnswer(invocation ->
                    jdbc.update("DELETE FROM asset_status WHERE id = 11"));
            when(mapper.deleteOrphan(12L)).thenAnswer(invocation -> {
                jdbc.update("DELETE FROM asset_status WHERE id = 12");
                throw new IllegalStateException("database failure");
            });
            ImageAssetService transactional = transactional(service(mapper, jdbc), dataSource);

            assertThrows(
                    IllegalStateException.class,
                    () -> transactional.deleteOrphans(List.of(11L, 12L)));

            assertTrue(Files.exists(firstFile));
            assertTrue(Files.exists(secondFile));
            assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM asset_status", Long.class));
        } finally {
            dataSource.destroy();
        }
    }

    private ImageAssetService service(ImageAssetMapper mapper, JdbcTemplate jdbc) {
        return new ImageAssetService(
                mapper,
                mock(UploadStorageService.class),
                mock(ImageVariantService.class),
                jdbc,
                tempDir.toString());
    }

    private void writeDeletionManifest(
            Path directory, long assetId, Path original, Path staged) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String originalPath = encoder.encodeToString(
                original.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
        String stagedPath = encoder.encodeToString(
                staged.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
        Files.writeString(
                directory.resolve("manifest.tsv"),
                "IMAGE_ASSET_DELETE_V1\t" + assetId + "\n"
                        + originalPath + "\t" + stagedPath + "\n",
                StandardCharsets.UTF_8);
    }

    private ImageAssetService transactional(
            ImageAssetService target, SingleConnectionDataSource dataSource) {
        ProxyFactory factory = new ProxyFactory(target);
        factory.addAdvice(new TransactionInterceptor(
                new DataSourceTransactionManager(dataSource),
                new AnnotationTransactionAttributeSource()));
        return (ImageAssetService) factory.getProxy();
    }

    private SingleConnectionDataSource referenceDataSource() {
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE post (
                  id INTEGER PRIMARY KEY, title TEXT, content TEXT, cover_url TEXT,
                  image_url TEXT, background_value TEXT
                )
                """);
        jdbc.execute("CREATE TABLE blog_info (id INTEGER PRIMARY KEY, avatar_url TEXT, bg_url TEXT)");
        return dataSource;
    }

    private ImageAsset asset(Long id, String originalPath, String status) {
        ImageAsset asset = new ImageAsset();
        asset.setId(id);
        asset.setOriginalPath(originalPath);
        asset.setReferenceStatus(status);
        asset.setPurpose("ARTICLE");
        return asset;
    }
}
