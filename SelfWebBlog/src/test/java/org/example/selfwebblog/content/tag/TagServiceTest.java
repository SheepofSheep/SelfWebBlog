package org.example.selfwebblog.content.tag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagServiceTest {

    @Test
    void parsesWhitespaceCaseAndChineseCommasWithoutDuplicates() {
        assertEquals(List.of("Vue", "Java", "生活"),
                TagService.parseNames(" Vue, Java，vue, 生活 "));
    }

    @Test
    void createsReadableOrStableFallbackSlugs() {
        assertEquals("vue-3", TagService.slugFor("Vue 3"));
        assertTrue(TagService.slugFor("生活").matches("tag-[a-f0-9]{10}"));
        assertEquals(TagService.slugFor("生活"), TagService.slugFor("生活"));
    }

    @Test
    void rejectsBlankAndOverlongNames() {
        assertThrows(IllegalArgumentException.class, () -> TagService.normalizeName("   "));
        assertThrows(IllegalArgumentException.class, () -> TagService.normalizeName("x".repeat(31)));
    }
}
