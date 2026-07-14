package org.example.selfwebblog.upload.asset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimizedImageServiceTest {
    @TempDir
    Path uploadRoot;

    @Test
    void createsAndReusesConstrainedWebpVariant() throws Exception {
        Path original = uploadRoot.resolve("cover.png");
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", original.toFile());
        OptimizedImageService service = new OptimizedImageService(
                uploadRoot.toString(), new ImageVariantService());

        Path first = service.optimize("/uploads/cover.png", 480);
        Path second = service.optimize("/uploads/cover.png", 480);

        assertEquals(first, second);
        assertTrue(Files.isRegularFile(first));
        assertEquals(480, ImageIO.read(first.toFile()).getWidth());
    }

    @Test
    void rejectsTraversalAndArbitraryWidths() throws Exception {
        OptimizedImageService service = new OptimizedImageService(
                uploadRoot.toString(), new ImageVariantService());

        assertThrows(IllegalArgumentException.class,
                () -> service.optimize("/uploads/../secret.txt", 480));
        assertThrows(IllegalArgumentException.class,
                () -> service.optimize("/uploads/image.png", 999));
    }
}
