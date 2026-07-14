package org.example.selfwebblog.upload.asset;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class ImageVariantService {
    public VariantPaths createVariants(BufferedImage original, Path originalFile) throws IOException {
        if (!ImageIO.getImageWritersByFormatName("webp").hasNext()) {
            throw new IllegalStateException("WebP writer is unavailable");
        }
        Path small = original.getWidth() > 480 ? writeVariant(original, originalFile, 480, "-480.webp") : null;
        Path large = original.getWidth() > 1280 ? writeVariant(original, originalFile, 1280, "-1280.webp") : null;
        return new VariantPaths(small, large);
    }

    public Path createVariant(BufferedImage original, Path originalFile, int requestedWidth)
            throws IOException {
        if (!ImageIO.getImageWritersByFormatName("webp").hasNext()) {
            throw new IllegalStateException("WebP writer is unavailable");
        }
        int width = Math.min(original.getWidth(), requestedWidth);
        return writeVariant(original, originalFile, width, "-" + requestedWidth + ".webp");
    }

    private Path writeVariant(BufferedImage source, Path originalFile, int width, String suffix)
            throws IOException {
        int height = Math.max(1, Math.round((float) source.getHeight() * width / source.getWidth()));
        int type = source.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resized = new BufferedImage(width, height, type);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();

        String base = originalFile.getFileName().toString().replaceFirst("\\.[^.]+$", "");
        Path destination = originalFile.resolveSibling(base + suffix);
        Path temporary = Files.createTempFile(originalFile.getParent(), base, ".webp.tmp");
        try {
            if (!ImageIO.write(resized, "webp", temporary.toFile())) {
                throw new IOException("WebP variant could not be encoded");
            }
            if (ImageIO.read(temporary.toFile()) == null) {
                throw new IOException("WebP variant could not be verified");
            }
            atomicMove(temporary, destination);
            return destination;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void atomicMove(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record VariantPaths(Path small, Path large) {
    }
}
