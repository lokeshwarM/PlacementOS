package com.placementos.backend.domain.service.storage;

import java.io.IOException;

/**
 * Storage abstraction for persisting and retrieving email attachment binary payloads.
 * Decouples binary storage (local disk, future S3/GCS) from PostgreSQL metadata.
 */
public interface AttachmentStorage {

    /**
     * Stores binary data and returns a provider-independent storage reference.
     *
     * @param storageKey  A logical or relative path key under which to store the binary.
     * @param data        The binary bytes.
     * @param contentType The MIME content type.
     * @return The storage reference string to be persisted in database metadata.
     * @throws IOException If an I/O error occurs.
     */
    String store(String storageKey, byte[] data, String contentType) throws IOException;

    /**
     * Loads the binary data corresponding to the given storage reference.
     *
     * @param storageReference The reference string originally returned by store().
     * @return The binary bytes.
     * @throws IOException If the reference does not exist or cannot be read.
     */
    byte[] load(String storageReference) throws IOException;

    /**
     * Checks if the binary exists in storage.
     *
     * @param storageReference The reference string.
     * @return true if exists, false otherwise.
     */
    boolean exists(String storageReference);

    /**
     * Deletes the binary from storage.
     *
     * @param storageReference The reference string.
     * @throws IOException If deletion fails.
     */
    void delete(String storageReference) throws IOException;
}
