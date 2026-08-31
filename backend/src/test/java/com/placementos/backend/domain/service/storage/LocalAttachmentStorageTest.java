package com.placementos.backend.domain.service.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class LocalAttachmentStorageTest {

    @TempDir
    Path tempDir;

    private LocalAttachmentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalAttachmentStorage(tempDir.toString());
    }

    @Test
    void storeAndLoad_success() throws IOException {
        byte[] data = "Hello World PDF Content".getBytes();
        String ref = storage.store("test/sample.pdf", data, "application/pdf");

        assertNotNull(ref);
        assertTrue(storage.exists(ref));

        byte[] loaded = storage.load(ref);
        assertArrayEquals(data, loaded);
    }

    @Test
    void pathTraversal_throwsSecurityException() {
        byte[] data = "Malicious file".getBytes();
        assertThrows(SecurityException.class, () -> {
            storage.store("../../etc/passwd", data, "text/plain");
        });
    }

    @Test
    void loadMissing_throwsIOException() {
        assertThrows(IOException.class, () -> {
            storage.load("non_existent_ref.pdf");
        });
    }

    @Test
    void delete_removesFile() throws IOException {
        byte[] data = "Temporary Content".getBytes();
        String ref = storage.store("temp/delete_me.txt", data, "text/plain");
        assertTrue(storage.exists(ref));

        storage.delete(ref);
        assertFalse(storage.exists(ref));
    }
}
