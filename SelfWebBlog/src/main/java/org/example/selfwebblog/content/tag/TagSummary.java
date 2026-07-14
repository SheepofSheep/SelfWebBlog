package org.example.selfwebblog.content.tag;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

public record TagSummary(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String name,
        String slug,
        long postCount,
        LocalDateTime latestPostTime) {
}
