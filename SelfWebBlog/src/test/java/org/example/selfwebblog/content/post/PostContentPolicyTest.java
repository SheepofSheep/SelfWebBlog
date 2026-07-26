package org.example.selfwebblog.content.post;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertFalse(PostContentPolicy.hasPublishableContent("<div></div><!-- 不可见 --><br>"));
        assertFalse(PostContentPolicy.hasPublishableContent("<style>body { color: red; }</style>"));
        assertFalse(PostContentPolicy.hasPublishableContent("<!-- ![](/uploads/hidden.webp) -->"));
        assertFalse(PostContentPolicy.hasPublishableContent(
                "<script><img src=\"https://media.example/hidden.webp\"></script>"));
        assertFalse(PostContentPolicy.hasPublishableContent("&nbsp;&#160;&#xA0;\u200B"));
        assertTrue(PostContentPolicy.hasPublishableContent("<p>可见正文</p>"));
    }

    @Test
    void publishedAndArchivedPostsBothRequireVisibleTextOrPermanentMedia() {
        assertThrows(IllegalArgumentException.class,
                () -> PostContentPolicy.requirePublishable("PUBLISHED", "  # **  "));
        assertThrows(IllegalArgumentException.class,
                () -> PostContentPolicy.requirePublishable("ARCHIVED", "![上传中](blob:task-1)"));

        assertDoesNotThrow(() -> PostContentPolicy.requirePublishable("PUBLISHED", "只有正文"));
        assertDoesNotThrow(() -> PostContentPolicy.requirePublishable("PUBLISHED", "![](/uploads/only-image.webp)"));
        assertDoesNotThrow(() -> PostContentPolicy.requirePublishable("ARCHIVED", "只有正文"));
        assertDoesNotThrow(() -> PostContentPolicy.requirePublishable("ARCHIVED", "![](/uploads/only-image.webp)"));
    }

    @Test
    void draftMayRemainEmpty() {
        assertDoesNotThrow(() -> PostContentPolicy.requirePublishable("DRAFT", "  # **  "));
    }
}
