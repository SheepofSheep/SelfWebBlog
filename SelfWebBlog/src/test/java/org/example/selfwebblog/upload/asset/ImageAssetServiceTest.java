package org.example.selfwebblog.upload.asset;

import org.example.selfwebblog.service.upload.UploadStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImageAssetServiceTest {
    @TempDir
    Path tempDir;

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
}
