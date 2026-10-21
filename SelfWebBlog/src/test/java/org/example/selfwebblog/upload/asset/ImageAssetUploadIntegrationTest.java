package org.example.selfwebblog.upload.asset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "blog.data-dir=target/test-data/asset-upload-regression")
class ImageAssetUploadIntegrationTest {
    @Autowired
    private ImageAssetService service;
    @Autowired
    private ImageAssetMapper mapper;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void resetAssets() {
        jdbc.update("DELETE FROM image_asset");
    }

    @Test
    void reuploadsReactivateOrphansAndRestoreMissingOriginalsWithoutChangingIdentity() throws Exception {
        ImageAssetResponse initial = service.storeArticleImage(image(), 9001L);
        mapper.updateReferenceStatus(initial.id(), "ORPHANED");

        ImageAssetResponse reused = service.storeArticleImage(image(), 9001L);
        assertEquals(initial.id(), reused.id());
        assertEquals(initial.originalUrl(), reused.originalUrl());
        assertEquals("ACTIVE", mapper.findById(initial.id()).getReferenceStatus());

        Path uploadRoot = Path.of("target/test-data/asset-upload-regression/uploads").toAbsolutePath().normalize();
        Path original = uploadRoot.resolve(initial.originalUrl().substring("/uploads/".length())).normalize();
        assertTrue(original.startsWith(uploadRoot));
        Files.delete(original);
        ImageAssetResponse restored = service.storeArticleImage(image(), 9001L);
        assertEquals(initial.id(), restored.id());
        assertEquals(initial.originalUrl(), restored.originalUrl());
        assertTrue(Files.isRegularFile(original));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM image_asset", Integer.class));
    }

    private MockMultipartFile image() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile("file", "asset.png", "image/png", bytes.toByteArray());
    }

    @Test
    void concurrentIdenticalUploadsReturnOneAsset() throws Exception {
        var results = concurrently(
                () -> service.storeArticleImage(image(), 9001L),
                () -> service.storeArticleImage(image(), 9001L));
        assertEquals(results.get(0).id(), results.get(1).id());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM image_asset", Integer.class));
    }

    @Test
    void concurrentReuploadAndDeletionKeepTheReturnedImageAvailable() throws Exception {
        ImageAssetResponse initial = service.storeArticleImage(image(), 9001L);
        mapper.updateReferenceStatus(initial.id(), "ORPHANED");
        var results = concurrently(
                () -> service.storeArticleImage(image(), 9001L),
                () -> {
                    try {
                        service.deleteOrphan(initial.id());
                    } catch (IllegalArgumentException alreadyActive) {
                        // Reactivation is allowed to win the race against deletion.
                    }
                    return null;
                });
        ImageAssetResponse uploaded = results.get(0);
        assertEquals("ACTIVE", mapper.findById(uploaded.id()).getReferenceStatus());
        Path root = Path.of("target/test-data/asset-upload-regression/uploads").toAbsolutePath().normalize();
        Path returnedFile = root.resolve(uploaded.originalUrl().substring("/uploads/".length())).normalize();
        assertTrue(returnedFile.startsWith(root));
        assertTrue(Files.isRegularFile(returnedFile));
    }

    private java.util.List<ImageAssetResponse> concurrently(
            Callable<ImageAssetResponse> first, Callable<ImageAssetResponse> second) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var futures = java.util.List.of(first, second).stream().map(action -> pool.submit(() -> {
                ready.countDown();
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return action.call();
            })).toList();
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            return java.util.Arrays.asList(
                    futures.get(0).get(10, TimeUnit.SECONDS), futures.get(1).get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }
}
