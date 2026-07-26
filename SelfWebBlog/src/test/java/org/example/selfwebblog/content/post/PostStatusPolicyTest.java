package org.example.selfwebblog.content.post;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostStatusPolicyTest {

    @Test
    void permitsOnlyTheSupportedLifecycle() {
        assertThat(PostStatusPolicy.canCreate("DRAFT")).isTrue();
        assertThat(PostStatusPolicy.canCreate("PUBLISHED")).isTrue();
        assertThat(PostStatusPolicy.canCreate("ARCHIVED")).isFalse();

        assertThat(PostStatusPolicy.canTransition("DRAFT", "PUBLISHED")).isTrue();
        assertThat(PostStatusPolicy.canTransition("PUBLISHED", "ARCHIVED")).isTrue();
        assertThat(PostStatusPolicy.canTransition("ARCHIVED", "PUBLISHED")).isTrue();
        assertThat(PostStatusPolicy.canTransition("PUBLISHED", "DRAFT")).isFalse();
        assertThat(PostStatusPolicy.canTransition("DRAFT", "ARCHIVED")).isFalse();
    }

    @Test
    void exposesOnlyPublishedPosts() {
        assertThat(PostStatusPolicy.isPublic("PUBLISHED")).isTrue();
        assertThat(PostStatusPolicy.isPublic("published")).isFalse();
        assertThat(PostStatusPolicy.isPublic(" PUBLISHED ")).isFalse();
        assertThat(PostStatusPolicy.isPublic("DRAFT")).isFalse();
        assertThat(PostStatusPolicy.isPublic("ARCHIVED")).isFalse();
        assertThat(PostStatusPolicy.isPublic(null)).isFalse();
    }

    @Test
    void rejectsMissingAndUnsupportedLifecycleStates() {
        assertThat(PostStatusPolicy.canCreate(null)).isFalse();
        assertThat(PostStatusPolicy.canTransition(null, "PUBLISHED")).isFalse();
        assertThat(PostStatusPolicy.canTransition("PUBLISHED", null)).isFalse();
        assertThat(PostStatusPolicy.canTransition("PUBLISHED", "DELETED")).isFalse();
    }
}
