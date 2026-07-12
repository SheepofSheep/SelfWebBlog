package org.example.selfwebblog.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.example.selfwebblog.entity.User;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class OAuthLoginTicketService {

    private final Cache<String, Long> tickets = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2))
            .maximumSize(2_000)
            .build();

    public String issue(User user) {
        String ticket = UUID.randomUUID().toString();
        tickets.put(ticket, user.getId());
        return ticket;
    }

    public Optional<Long> consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(tickets.asMap().remove(ticket));
    }
}
