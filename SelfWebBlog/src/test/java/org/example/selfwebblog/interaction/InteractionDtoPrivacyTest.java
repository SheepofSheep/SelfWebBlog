package org.example.selfwebblog.interaction;

import org.example.selfwebblog.interaction.dto.InteractionItemResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionDtoPrivacyTest {

    @Test
    void publicResponseCannotExposePrivateIpMaterial() {
        assertThat(InteractionItemResponse.class.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("ipAddress", "ipCiphertext", "ipHash");
    }
}
