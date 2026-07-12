package org.example.selfwebblog.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationPolicyTest {

    @Test
    void acceptsNormalPagination() {
        PaginationPolicy.PageRequest request = PaginationPolicy.require(2, 40);

        assertThat(request.page()).isEqualTo(2);
        assertThat(request.size()).isEqualTo(40);
    }

    @Test
    void rejectsInvalidPageAndOversizedRequests() {
        assertThatThrownBy(() -> PaginationPolicy.require(0, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PaginationPolicy.require(1, 101))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
