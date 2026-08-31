package com.placementos.backend.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Validates Google-signed JWTs delivered in authenticated Pub/Sub push requests.
 *
 * <p>Google Cloud Pub/Sub authenticated push subscriptions attach a signed JWT in the
 * {@code Authorization: Bearer <jwt>} header of each push delivery. This component
 * verifies that a received JWT is a genuine, unexpired token issued by Google for
 * this specific subscription.
 *
 * <p><b>Validation steps (per Google's push documentation):</b>
 * <ol>
 *   <li>Signature — verified against Google's public keys via {@link GoogleIdTokenVerifier}</li>
 *   <li>Issuer — must be {@code https://accounts.google.com}</li>
 *   <li>Expiration — must not be in the past (handled by {@link GoogleIdTokenVerifier})</li>
 *   <li>Audience — must match the configured push endpoint URL
 *       ({@code app.google.pubsub.push-endpoint})</li>
 *   <li>Service account email — must match the configured expected identity
 *       ({@code app.google.pubsub.expected-service-account-email})</li>
 *   <li>email_verified — must be {@code true}</li>
 * </ol>
 *
 * <p><b>Security note:</b> The raw JWT string and its decoded claims are never logged.
 * Validation failures are reported only as opaque rejection reasons suitable for logs.
 *
 * <p><b>Authentication vs. authorisation distinction:</b>
 * The OAuth2 authorize/callback endpoints use a different flow (CSRF state + Google
 * authorization-code exchange). The Pub/Sub push endpoint uses this JWT-based mechanism
 * exclusively. These two flows must not be confused or merged.
 */
@Component
public class PubSubJwtValidator {

    private static final String GOOGLE_ISSUER = "https://accounts.google.com";

    @Value("${app.google.pubsub.push-endpoint}")
    private String expectedAudience;

    @Value("${app.google.pubsub.expected-service-account-email}")
    private String expectedServiceAccountEmail;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() throws GeneralSecurityException, IOException {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance())
                // Audience must match the push endpoint URL configured in the subscription
                .setAudience(Collections.singletonList(expectedAudience))
                .build();
    }

    /**
     * Validates a raw JWT string from a Pub/Sub push Authorization header.
     *
     * @param rawJwt the token value (without the "Bearer " prefix)
     * @return {@code true} if all validation checks pass; {@code false} otherwise
     */
    public boolean isValid(String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            return false;
        }

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(rawJwt);
        } catch (Exception e) {
            // Covers signature failure, malformed token, network errors fetching certs
            return false;
        }

        if (idToken == null) {
            // null means verification failed (expired, wrong audience, bad signature)
            return false;
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        // Validate issuer
        if (!GOOGLE_ISSUER.equals(payload.getIssuer())) {
            return false;
        }

        // Validate email_verified
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            return false;
        }

        // Validate service account identity
        String email = payload.getEmail();
        if (email == null || !email.equals(expectedServiceAccountEmail)) {
            return false;
        }

        return true;
    }
}
