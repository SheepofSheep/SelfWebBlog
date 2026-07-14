package org.example.selfwebblog.upload.asset;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImageAsset {
    private Long id;
    private String contentHash;
    private String purpose;
    private String originalPath;
    private String smallWebpPath;
    private String largeWebpPath;
    private String originalMime;
    private int width;
    private int height;
    private long byteSize;
    private Long uploaderId;
    private String referenceStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
