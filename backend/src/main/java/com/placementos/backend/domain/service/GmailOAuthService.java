package com.placementos.backend.domain.service;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Profile;
import com.placementos.backend.domain.model.GmailSource;
import com.placementos.backend.domain.repository.GmailSourceRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class GmailOAuthService {

    private static final String REDIS_STATE_PREFIX = "oauth2:state:";
    private static final long STATE_TTL_MINUTES = 10;
    
    private final StringRedisTemplate redisTemplate;
    private final GmailSourceRepository gmailSourceRepository;
    
    @Value("${app.google.oauth.client-id}")
    private String clientId;

    @Value("${app.google.oauth.client-secret}")
    private String clientSecret;

    @Value("${app.google.oauth.redirect-uri}")
    private String redirectUri;

    private NetHttpTransport httpTransport;
    private GsonFactory jsonFactory;
    private GoogleAuthorizationCodeFlow flow;

    public GmailOAuthService(StringRedisTemplate redisTemplate, GmailSourceRepository gmailSourceRepository) {
        this.redisTemplate = redisTemplate;
        this.gmailSourceRepository = gmailSourceRepository;
    }

    @PostConstruct
    public void init() throws GeneralSecurityException, IOException {
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        this.jsonFactory = GsonFactory.getDefaultInstance();

        GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
        web.setClientId(clientId);
        web.setClientSecret(clientSecret);
        
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setWeb(web);

        this.flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, jsonFactory, clientSecrets, Collections.singleton(GmailScopes.GMAIL_READONLY))
                .setAccessType("offline")
                .setApprovalPrompt("force") // Force approval to ensure we always get a refresh token
                .build();
    }

    public String generateAuthorizationUrl() {
        String state = generateSecureState();
        
        // Store state in Redis to protect against CSRF
        redisTemplate.opsForValue().set(REDIS_STATE_PREFIX + state, "valid", STATE_TTL_MINUTES, TimeUnit.MINUTES);

        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri);
        authorizationUrl.setState(state);
        return authorizationUrl.build();
    }

    public GmailSource handleCallback(String code, String state) throws IOException {
        // 1. Validate State
        validateState(state);

        // 2. Exchange authorization code for token
        TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();

        // 3. Obtain authenticated Gmail account identity
        Gmail gmail = new Gmail.Builder(httpTransport, jsonFactory, flow.createAndStoreCredential(response, "user"))
                .setApplicationName("PlacementOS")
                .build();
        
        Profile profile = gmail.users().getProfile("me").execute();
        String emailAddress = profile.getEmailAddress();

        // 4. Persist or update the source mailbox registration
        return saveOrUpdateGmailSource(emailAddress, response.toString());
    }

    private String generateSecureState() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void validateState(String state) {
        if (state == null || state.isEmpty()) {
            throw new IllegalArgumentException("OAuth state parameter is missing.");
        }
        
        String key = REDIS_STATE_PREFIX + state;
        Boolean exists = redisTemplate.hasKey(key);
        
        if (Boolean.TRUE.equals(exists)) {
            // State is valid, remove it so it cannot be reused (one-time use)
            redisTemplate.delete(key);
        } else {
            throw new IllegalArgumentException("Invalid or expired OAuth state parameter.");
        }
    }

    private GmailSource saveOrUpdateGmailSource(String emailAddress, String credentialJson) {
        Optional<GmailSource> existingSource = gmailSourceRepository.findByEmailAddress(emailAddress);
        
        GmailSource source;
        if (existingSource.isPresent()) {
            source = existingSource.get();
        } else {
            source = new GmailSource();
            source.setEmailAddress(emailAddress);
            source.setProvider("GOOGLE");
        }
        
        // In local development, we store the credential JSON in the DB.
        // In production, this should be encrypted using KMS or moved to Secret Manager.
        source.setCredential(credentialJson);
        source.setStatus("ACTIVE");
        
        return gmailSourceRepository.save(source);
    }
}
