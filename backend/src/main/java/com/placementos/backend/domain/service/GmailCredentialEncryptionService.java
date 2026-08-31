package com.placementos.backend.domain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class GmailCredentialEncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final String FORMAT_VERSION = "v1";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public GmailCredentialEncryptionService(@Value("${app.google.oauth.encryption-key}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("Encryption key must not be blank.");
        }
        
        byte[] decodedKey = Base64.getDecoder().decode(base64Key);
        if (decodedKey.length != 32) {
            throw new IllegalArgumentException("Encryption key must be exactly 32 bytes (256 bits) for AES-256.");
        }
        
        this.secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     * The output format is: "v1:{base64-iv}:{base64-ciphertext-with-tag}"
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("Plaintext to encrypt cannot be null or empty");
        }

        try {
            // Generate unique IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            String encodedIv = Base64.getEncoder().withoutPadding().encodeToString(iv);
            String encodedCiphertext = Base64.getEncoder().withoutPadding().encodeToString(ciphertext);
            
            return FORMAT_VERSION + ":" + encodedIv + ":" + encodedCiphertext;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt credential", e);
        }
    }

    /**
     * Decrypts the formatted ciphertext back to plaintext.
     */
    public String decrypt(String encryptedPayload) {
        if (encryptedPayload == null || encryptedPayload.isEmpty()) {
            throw new IllegalArgumentException("Encrypted payload cannot be null or empty");
        }

        String[] parts = encryptedPayload.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid encrypted payload format");
        }
        
        String version = parts[0];
        if (!FORMAT_VERSION.equals(version)) {
            throw new IllegalArgumentException("Unsupported encryption version: " + version);
        }
        
        try {
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[2]);
            
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            return new String(plaintext, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt credential. It may have been tampered with or the key is incorrect.", e);
        }
    }
}
