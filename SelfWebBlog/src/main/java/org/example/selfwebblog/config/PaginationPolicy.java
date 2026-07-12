package org.example.selfwebblog.config;

public final class PaginationPolicy {

    public static final int MAX_PAGE_SIZE = 100;

    private PaginationPolicy() {}

    public static PageRequest require(int page, int size) {
        if (page < 1) {
            throw new IllegalArgumentException("页码不能小于 1");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("每页数量必须在 1-" + MAX_PAGE_SIZE + " 之间");
        }
        return new PageRequest(page, size);
    }

    public record PageRequest(int page, int size) {}
}
