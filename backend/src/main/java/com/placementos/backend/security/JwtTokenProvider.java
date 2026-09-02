package com.placementos.backend.security;

import com.placementos.backend.domain.enums.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Utility for generating and securely verifying HMAC-SHA256 JWT tokens.
 * Zero external library dependencies, fully standard Java crypto.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret:placementos-production-grade-hmac-sha256-secret-key-must-be-very-long-and-secure-2026}") String secretKey,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = secretKey;
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT token containing user identity claims.
     */
    public String generateToken(Long userId, String email, UserRole role, Long studentId) {
        long now = System.currentTimeMillis();
        long exp = now + expirationMs;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String studentIdJson = studentId != null ? String.valueOf(studentId) : "null";
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"userId\":%d,\"role\":\"%s\",\"studentId\":%s,\"iat\":%d,\"exp\":%d}",
                email, userId, role.name(), studentIdJson, now / 1000, exp / 1000
        );

        String encodedHeader = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signature = sign(encodedHeader + "." + encodedPayload, secretKey);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    /**
     * Validates signature and expiry of the JWT token.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            String content = parts[0] + "." + parts[1];
            String expectedSignature = sign(content, secretKey);

            if (!MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.UTF_8), expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("JWT signature mismatch");
                return false;
            }

            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            long exp = extractLongClaim(payloadJson, "exp");
            if (exp > 0 && System.currentTimeMillis() / 1000 > exp) {
                log.warn("JWT token has expired");
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
            return false;
        }
    }

    public String getEmailFromToken(String token) {
        return extractStringClaimFromPayload(token, "sub");
    }

    public Long getUserIdFromToken(String token) {
        return extractLongClaimFromPayload(token, "userId");
    }

    public String getRoleFromToken(String token) {
        return extractStringClaimFromPayload(token, "role");
    }

    public Long getStudentIdFromToken(String token) {
        return extractLongClaimFromPayload(token, "studentId");
    }

    private String extractStringClaimFromPayload(String token, String claim) {
        try {
            String[] parts = token.split("\\.");
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            return extractStringClaim(payloadJson, claim);
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractLongClaimFromPayload(String token, String claim) {
        try {
            String[] parts = token.split("\\.");
            String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
            return extractLongClaim(payloadJson, claim);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractStringClaim(String json, String claim) {
        String pattern = "\"" + claim + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        int start = idx + pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private long extractLongClaim(String json, String claim) {
        String pattern = "\"" + claim + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return -1;
        int start = idx + pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (Exception e) {
            return -1;
        }
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(hmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", e);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64UrlDecode(String str) {
        return Base64.getUrlDecoder().decode(str);
    }
}
