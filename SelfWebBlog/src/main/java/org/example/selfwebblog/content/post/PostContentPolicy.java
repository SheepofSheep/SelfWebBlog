package org.example.selfwebblog.content.post;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PostContentPolicy {
    private static final Pattern MARKDOWN_MEDIA = Pattern.compile(
            "!\\[[^]]*]\\(([^\\s)]+)(?:\\s+\"[^\"]*\")?\\)");
    private static final Pattern HTML_MEDIA = Pattern.compile(
            "<(?:img|video|audio)\\b[^>]*(?:src|poster)=['\"]([^'\"]+)['\"][^>]*>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_ONLY = Pattern.compile("[#>*_`~\\-\\[\\]()!\\s]");

    private PostContentPolicy() {
    }

    public static boolean hasPublishableContent(String raw) {
        String content = raw == null ? "" : raw;
        if (containsPermanentMedia(MARKDOWN_MEDIA.matcher(content))
                || containsPermanentMedia(HTML_MEDIA.matcher(content))) {
            return true;
        }
        String withoutMedia = MARKDOWN_MEDIA.matcher(content).replaceAll("");
        withoutMedia = HTML_MEDIA.matcher(withoutMedia).replaceAll("");
        String visible = MARKDOWN_ONLY.matcher(withoutMedia).replaceAll("");
        return !visible.isBlank();
    }

    public static void requirePublishable(String status, String content) {
        if ("PUBLISHED".equalsIgnoreCase(status) && !hasPublishableContent(content)) {
            throw new IllegalArgumentException("文章需要包含正文或媒体");
        }
    }

    private static boolean containsPermanentMedia(Matcher matcher) {
        while (matcher.find()) {
            String url = matcher.group(1);
            if (url.startsWith("/uploads/")
                    || url.startsWith("/api/media/")
                    || url.startsWith("https://")
                    || url.startsWith("http://")) {
                return true;
            }
        }
        return false;
    }
}
