package org.example.selfwebblog.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
public class OAuthStateService {
    private final Cache<String, Boolean> states = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10))
            .maximumSize(10_000)
            .build();

    public String issue() {
        String state = UUID.randomUUID().toString();
        states.put(state, Boolean.TRUE);
        return state;
    }

    public boolean consume(String state, String browserState) {
        if (state == null || !Objects.equals(state, browserState)) return false;
        if (states.asMap().remove(state) == null) return false;
        return true;
    }
}
