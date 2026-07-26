package org.example.selfwebblog.interaction;

import org.example.selfwebblog.interaction.dto.InteractionItemResponse;
import org.example.selfwebblog.interaction.dto.AdminInteractionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    @Test
    void adminInteractionIdsRemainExactInJavascriptClients() throws Exception {
        AdminInteractionResponse response = new AdminInteractionResponse(
                9007199254740993L, "POST", 9007199254740995L, null, null,
                "content", 9007199254740997L, "name", "", "USER", "", "default",
                "PUBLISHED", "unknown", 0, false, null);

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(response);

        assertThat(json).contains("\"id\":\"9007199254740993\"")
                .contains("\"targetId\":\"9007199254740995\"")
                .contains("\"userId\":\"9007199254740997\"");
    }
}
