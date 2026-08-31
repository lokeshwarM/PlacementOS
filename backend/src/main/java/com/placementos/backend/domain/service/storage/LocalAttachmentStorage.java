package com.placementos.backend.domain.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Local filesystem implementation of {@link AttachmentStorage}.
 * Provides strict path traversal protection and directory isolation.
 */
@Service
public class LocalAttachmentStorage implements AttachmentStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalAttachmentStorage.class);
    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024; // 25 MB

    private final Path baseStorageDir;

    public LocalAttachmentStorage(@Value("${app.attachment.storage-dir:./data/attachments}") String storageDirPath) {
        this.baseStorageDir = Paths.get(storageDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStorageDir);
            log.info("Initialized LocalAttachmentStorage at: {}", this.baseStorageDir);
        } catch (IOException e) {
            log.error("Failed to initialize attachment storage directory: {}", this.baseStorageDir, e);
            throw new IllegalStateException("Cannot create attachment storage directory", e);
        }
    }

    @Override
    public String store(String storageKey, byte[] data, String contentType) throws IOException {
        Objects.requireNonNull(storageKey, "storageKey must not be null");
        Objects.requireNonNull(data, "data must not be null");

        if (data.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Attachment exceeds maximum allowed size of 25MB (" + data.length + " bytes)");
        }

        // Sanitize key and prevent directory traversal
        String safeKey = sanitizeStorageKey(storageKey);
        Path targetPath = baseStorageDir.resolve(safeKey).normalize();

        if (!targetPath.startsWith(baseStorageDir)) {
            throw new SecurityException("Invalid storage key resulting in path traversal: " + storageKey);
        }

        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }

        Files.write(targetPath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        log.debug("Stored attachment binary to {} ({} bytes)", safeKey, data.length);

        return safeKey;
    }

    @Override
    public byte[] load(String storageReference) throws IOException {
        Objects.requireNonNull(storageReference, "storageReference must not be null");

        String safeKey = sanitizeStorageKey(storageReference);
        Path targetPath = baseStorageDir.resolve(safeKey).normalize();

        if (!targetPath.startsWith(baseStorageDir)) {
            throw new SecurityException("Invalid storage reference resulting in path traversal: " + storageReference);
        }

        if (!Files.exists(targetPath)) {
            throw new IOException("Attachment binary not found at storage reference: " + storageReference);
        }

        return Files.readAllBytes(targetPath);
    }

    @Override
    public boolean exists(String storageReference) {
        if (storageReference == null || storageReference.isBlank()) {
            return false;
        }
        try {
            String safeKey = sanitizeStorageKey(storageReference);
            Path targetPath = baseStorageDir.resolve(safeKey).normalize();
            return targetPath.startsWith(baseStorageDir) && Files.exists(targetPath);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void delete(String storageReference) throws IOException {
        if (storageReference == null || storageReference.isBlank()) {
            return;
        }
        String safeKey = sanitizeStorageKey(storageReference);
        Path targetPath = baseStorageDir.resolve(safeKey).normalize();

        if (!targetPath.startsWith(baseStorageDir)) {
            throw new SecurityException("Invalid storage reference resulting in path traversal: " + storageReference);
        }

        Files.deleteIfExists(targetPath);
    }

    private String sanitizeStorageKey(String key) {
        // Normalize slashes to forward slashes and strip leading slashes
        String normalized = key.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        // Disallow parent path elements
        if (normalized.contains("../") || normalized.contains("/..") || normalized.equals("..")) {
            throw new SecurityException("Storage key contains illegal path traversal segments ('..'): " + key);
        }
        return normalized;
    }
}
