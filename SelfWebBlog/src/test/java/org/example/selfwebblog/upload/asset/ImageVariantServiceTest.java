package org.example.selfwebblog.upload.asset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageVariantServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void createsWebpVariantsWithoutUpscaling() throws Exception {
        ImageVariantService service = new ImageVariantService();
        BufferedImage image = new BufferedImage(1600, 900, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, new Color(255, 0, 0, 80).getRGB());
        Path original = tempDir.resolve("article.png");
        ImageIO.write(image, "png", original.toFile());

        ImageVariantService.VariantPaths variants = service.createVariants(image, original);

        assertTrue(variants.small().toFile().isFile());
        assertTrue(variants.large().toFile().isFile());
        BufferedImage small = ImageIO.read(variants.small().toFile());
        assertNotNull(small);
        assertEquals(480, small.getWidth());

        BufferedImage tiny = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
        ImageVariantService.VariantPaths tinyVariants = service.createVariants(
                tiny, tempDir.resolve("tiny.jpg"));
        assertNull(tinyVariants.small());
        assertNull(tinyVariants.large());
    }
}
