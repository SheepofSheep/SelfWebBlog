package org.example.selfwebblog.upload.asset;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

@RestController
@RequestMapping("/api/media")
public class OptimizedImageController {
    private final OptimizedImageService imageService;

    public OptimizedImageController(OptimizedImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping(value = "/optimized", produces = "image/webp")
    public ResponseEntity<Resource> optimized(
            @RequestParam String path,
            @RequestParam(defaultValue = "480") int width) throws IOException {
        Path optimized = imageService.optimize(path, width);
        Resource resource = new FileSystemResource(optimized);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("image/webp"))
                .contentLength(resource.contentLength())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(resource);
    }
}
