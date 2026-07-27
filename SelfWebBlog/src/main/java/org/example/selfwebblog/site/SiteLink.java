package org.example.selfwebblog.site;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

public record SiteLink(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String label,
        String url,
        String iconKey,
        boolean enabled,
        int sortOrder,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
