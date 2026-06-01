package org.example.selfwebblog.service.upload;

public record StoredUpload(String filename, String url, String extension, long size) {
}
