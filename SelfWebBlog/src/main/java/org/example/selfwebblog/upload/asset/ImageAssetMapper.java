package org.example.selfwebblog.upload.asset;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImageAssetMapper {
    @Select("""
            SELECT * FROM image_asset
            WHERE uploader_id = #{uploaderId} AND content_hash = #{hash} AND purpose = #{purpose}
              AND reference_status = 'ACTIVE'
            LIMIT 1
            """)
    ImageAsset findReusable(@Param("uploaderId") Long uploaderId,
                            @Param("hash") String hash,
                            @Param("purpose") String purpose);

    @Insert("""
            INSERT INTO image_asset(
                content_hash, purpose, original_path, small_webp_path, large_webp_path,
                original_mime, width, height, byte_size, uploader_id, reference_status
            ) VALUES (
                #{contentHash}, #{purpose}, #{originalPath}, #{smallWebpPath}, #{largeWebpPath},
                #{originalMime}, #{width}, #{height}, #{byteSize}, #{uploaderId}, 'ACTIVE'
            )
            """)
    int insertAsset(ImageAsset asset);

    @Select("""
            SELECT * FROM image_asset
            WHERE uploader_id = #{uploaderId} AND purpose = #{purpose} AND reference_status = 'ACTIVE'
            ORDER BY create_time DESC LIMIT 100
            """)
    List<ImageAsset> listAssets(@Param("uploaderId") Long uploaderId,
                                @Param("purpose") String purpose);
}
