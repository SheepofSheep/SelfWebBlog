package org.example.selfwebblog.service.upload;

public enum UploadTarget {
    ARTICLE_IMAGE("", "/uploads/"),
    AVATAR("avatars", "/uploads/avatars/");

    private final String subdirectory;
    private final String urlPrefix;

    UploadTarget(String subdirectory, String urlPrefix) {
        this.subdirectory = subdirectory;
        this.urlPrefix = urlPrefix;
    }

    public String subdirectory() {
        return subdirectory;
    }

    public String urlPrefix() {
        return urlPrefix;
    }
}
