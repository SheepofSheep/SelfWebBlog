package org.example.selfwebblog.security;

import org.example.selfwebblog.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthLoginTicketServiceTest {

    @Test
    void ticketCanOnlyBeConsumedOnce() {
        OAuthLoginTicketService service = new OAuthLoginTicketService();
        User user = new User();
        user.setId(7L);

        String ticket = service.issue(user);

        assertThat(service.consume(ticket)).contains(7L);
        assertThat(service.consume(ticket)).isEmpty();
    }
}
