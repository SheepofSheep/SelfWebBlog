package org.example.selfwebblog.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthStateServiceTest {
    @Test
    void stateMustMatchTheIssuingBrowserAndCanOnlyBeConsumedOnce() {
        OAuthStateService service = new OAuthStateService();
        String state = service.issue();

        assertThat(service.consume(state, "another-browser-state")).isFalse();
        assertThat(service.consume(state, state)).isTrue();
        assertThat(service.consume(state, state)).isFalse();
    }
}
