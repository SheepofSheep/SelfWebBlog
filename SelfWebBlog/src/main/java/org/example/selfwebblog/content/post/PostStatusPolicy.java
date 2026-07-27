package org.example.selfwebblog.content.post;

import java.util.Locale;
import java.util.Set;

public final class PostStatusPolicy {
    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String ARCHIVED = "ARCHIVED";

    private static final Set<String> SUPPORTED = Set.of(DRAFT, PUBLISHED, ARCHIVED);

    private PostStatusPolicy() {
    }

    public static String normalize(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String status) {
        return SUPPORTED.contains(normalize(status));
    }

    public static boolean canCreate(String status) {
        String normalized = normalize(status);
        return DRAFT.equals(normalized) || PUBLISHED.equals(normalized);
    }

    public static boolean canTransition(String from, String to) {
        String source = normalize(from);
        String target = normalize(to);
        if (!isValid(source) || !isValid(target)) return false;
        return source.equals(target)
                || (DRAFT.equals(source) && PUBLISHED.equals(target))
                || (PUBLISHED.equals(source) && ARCHIVED.equals(target))
                || (ARCHIVED.equals(source) && PUBLISHED.equals(target));
    }

    public static boolean isPublic(String status) {
        return PUBLISHED.equals(status);
    }
}
