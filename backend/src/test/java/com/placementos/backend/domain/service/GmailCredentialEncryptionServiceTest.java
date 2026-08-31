package com.placementos.backend.domain.service;

import org.junit.jupiter.api.Test;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class GmailCredentialEncryptionServiceTest {

    // Valid 32 byte base64 key
    private final String validKey = Base64.getEncoder().encodeToString(new byte[32]);
    private final GmailCredentialEncryptionService encryptionService = new GmailCredentialEncryptionService(validKey);

    @Test
    public void testEncryptDecryptRoundtrip() {
        String plaintext = "my-secret-refresh-token";
        String ciphertext = encryptionService.encrypt(plaintext);
        
        assertNotNull(ciphertext);
        assertTrue(ciphertext.startsWith("v1:"));
        assertNotEquals(plaintext, ciphertext);
        
        String decrypted = encryptionService.decrypt(ciphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testDifferentEncryptionProducesDifferentCiphertext() {
        String plaintext = "my-secret-refresh-token";
        String ciphertext1 = encryptionService.encrypt(plaintext);
        String ciphertext2 = encryptionService.encrypt(plaintext);
        
        assertNotEquals(ciphertext1, ciphertext2, "AES-GCM with secure random IV must produce different ciphertexts for the same plaintext");
    }

    @Test
    public void testTamperedCiphertextFails() {
        String plaintext = "my-secret-refresh-token";
        String ciphertext = encryptionService.encrypt(plaintext);
        
        // Tamper with ciphertext by modifying a character at the end
        String tampered = ciphertext.substring(0, ciphertext.length() - 1) + (ciphertext.endsWith("a") ? "b" : "a");
        
        Exception exception = assertThrows(RuntimeException.class, () -> encryptionService.decrypt(tampered));
        assertTrue(exception.getMessage().contains("Failed to decrypt"));
    }

    @Test
    public void testInvalidKeyThrowsException() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
        assertThrows(IllegalArgumentException.class, () -> new GmailCredentialEncryptionService(shortKey));
    }
    
    @Test
    public void testWrongKeyFailsDecryption() {
        String plaintext = "my-secret-refresh-token";
        String ciphertext = encryptionService.encrypt(plaintext);
        
        // Try decrypting with a different valid key
        byte[] differentKeyBytes = new byte[32];
        differentKeyBytes[0] = 1; // Different key
        String differentKey = Base64.getEncoder().encodeToString(differentKeyBytes);
        GmailCredentialEncryptionService wrongKeyService = new GmailCredentialEncryptionService(differentKey);
        
        assertThrows(RuntimeException.class, () -> wrongKeyService.decrypt(ciphertext));
    }

    @Test
    public void testInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> encryptionService.decrypt("invalid-format"));
        assertThrows(IllegalArgumentException.class, () -> encryptionService.decrypt("v2:iv:data"));
    }
}
