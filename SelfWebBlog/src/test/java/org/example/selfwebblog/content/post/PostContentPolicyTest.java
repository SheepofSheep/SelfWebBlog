package org.example.selfwebblog.content.post;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostContentPolicyTest {

    @Test
    void acceptsVisibleTextWithoutRequiredMetadata() {
        assertTrue(PostContentPolicy.hasPublishableContent("一段正文"));
        assertTrue(PostContentPolicy.hasPublishableContent("```java\nreturn true;\n```"));
        assertTrue(PostContentPolicy.hasPublishableContent("`inlineCode()`"));
    }

    @Test
    void acceptsPermanentMarkdownAndHtmlMedia() {
        assertTrue(PostContentPolicy.hasPublishableContent("![](/uploads/a.webp)"));
        assertTrue(PostContentPolicy.hasPublishableContent("<video src=\"https://media.example/a.mp4\"></video>"));
        assertTrue(PostContentPolicy.hasPublishableContent("<picture><img src=\"/api/media/optimized?id=1\"></picture>"));
    }

    @Test
    void rejectsEmptyFormattingAndTemporaryUploadTokens() {
        assertFalse(PostContentPolicy.hasPublishableContent(" \n# ** ` ` ---"));
        assertFalse(PostContentPolicy.hasPublishableContent("![上传中](blob:task-1)"));
        assertFalse(PostContentPolicy.hasPublishableContent("![上传失败](upload-failed:task-1)"));
    }
}
