package org.example.selfwebblog.interaction;

import java.util.Map;
import java.util.Set;

public final class InteractionPolicy {
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "PENDING", Set.of("PUBLISHED", "SPAM"),
            "PUBLISHED", Set.of("HIDDEN", "SPAM"),
            "HIDDEN", Set.of("PUBLISHED"),
            "SPAM", Set.of("HIDDEN")
    );

    private InteractionPolicy() {
    }

    public static boolean canTransition(String current, String next) {
        return TRANSITIONS.getOrDefault(current, Set.of()).contains(next);
    }
}
