package org.example.selfwebblog.upload.asset;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

public record ImageAssetResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String originalUrl,
        String smallWebpUrl,
        String largeWebpUrl,
        int width,
        int height,
        long byteSize,
        String contentHash,
        String markdown) {

    public static ImageAssetResponse from(ImageAsset asset) {
        String markdown = "![图片描述](" + asset.getOriginalPath() + ")";
        return new ImageAssetResponse(
                asset.getId(), asset.getOriginalPath(), asset.getSmallWebpPath(),
                asset.getLargeWebpPath(), asset.getWidth(), asset.getHeight(),
                asset.getByteSize(), asset.getContentHash(), markdown);
    }
}
