package com.placementos.backend.domain.service;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class GmailOAuthService {

    private static final String REDIS_STATE_PREFIX = "oauth2:state:";
    private static final long STATE_TTL_MINUTES = 10;
    
    private final StringRedisTemplate redisTemplate;
    private final GmailSourceRepository gmailSourceRepository;
    private final GmailCredentialEncryptionService encryptionService;
    
    @Value("${app.google.oauth.client-id}")
    private String clientId;

    @Value("${app.google.oauth.client-secret}")
    private String clientSecret;

    @Value("${app.google.oauth.redirect-uri}")
    private String redirectUri;

    private NetHttpTransport httpTransport;
    private GsonFactory jsonFactory;
    private GoogleAuthorizationCodeFlow flow;

    public GmailOAuthService(StringRedisTemplate redisTemplate, 
                             GmailSourceRepository gmailSourceRepository,
                             GmailCredentialEncryptionService encryptionService) {
        this.redisTemplate = redisTemplate;
        this.gmailSourceRepository = gmailSourceRepository;
        this.encryptionService = encryptionService;
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
                
        // Perform credential migration on startup if needed
        migratePlaintextCredentials();
    }

    /**
     * Migrates any plaintext JSON credentials to the encrypted format securely on startup.
     */
    @Transactional
    protected void migratePlaintextCredentials() {
        List<GmailSource> sources = gmailSourceRepository.findAll();
        for (GmailSource source : sources) {
            String credential = source.getCredential();
            if (credential != null && credential.trim().startsWith("{")) {
                try {
                    // Extract refresh token from plaintext JSON
                    TokenResponse tokenResponse = jsonFactory.fromString(credential, TokenResponse.class);
                    String refreshToken = tokenResponse.getRefreshToken();
                    
                    if (refreshToken != null) {
                        String encryptedRefreshToken = encryptionService.encrypt(refreshToken);
                        source.setCredential(encryptedRefreshToken);
                        gmailSourceRepository.save(source);
                        // Log safely
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to migrate plaintext credential for: " + source.getEmailAddress(), e);
                }
            }
        }
    }

    public String generateAuthorizationUrl() {
        String state = generateSecureState();
        
        // Store state in Redis to protect against CSRF
        redisTemplate.opsForValue().set(REDIS_STATE_PREFIX + state, "valid", STATE_TTL_MINUTES, TimeUnit.MINUTES);

        AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(redirectUri);
        authorizationUrl.setState(state);
        return authorizationUrl.build();
    }

    @Transactional
    public GmailSource handleCallback(String code, String state) throws IOException {
        // 1. Validate State
        validateState(state);

        // 2. Exchange authorization code for token
        TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();
        
        if (response.getRefreshToken() == null) {
            throw new IllegalArgumentException("No refresh token provided by Google. User may need to revoke access and re-authenticate.");
        }

        // 3. Obtain authenticated Gmail account identity
        Gmail gmail = new Gmail.Builder(httpTransport, jsonFactory, flow.createAndStoreCredential(response, "user"))
                .setApplicationName("PlacementOS")
                .build();
        
        Profile profile = gmail.users().getProfile("me").execute();
        String emailAddress = profile.getEmailAddress();

        // 4. Persist or update the source mailbox registration securely
        // Only the refresh token is stored. The access token is discarded from durable storage.
        String encryptedRefreshToken = encryptionService.encrypt(response.getRefreshToken());
        return saveOrUpdateGmailSource(emailAddress, encryptedRefreshToken);
    }
    
    /**
     * Constructs a Google API client for the given email address by decrypting the stored refresh token.
     */
    public Gmail getGmailClientForSource(String emailAddress) {
        GmailSource source = gmailSourceRepository.findByEmailAddress(emailAddress)
                .orElseThrow(() -> new IllegalArgumentException("Gmail source not found for: " + emailAddress));
                
        String refreshToken = encryptionService.decrypt(source.getCredential());
        
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);
                
        return new Gmail.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("PlacementOS")
                .build();
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

    private GmailSource saveOrUpdateGmailSource(String emailAddress, String encryptedRefreshToken) {
        Optional<GmailSource> existingSource = gmailSourceRepository.findByEmailAddress(emailAddress);
        
        GmailSource source;
        if (existingSource.isPresent()) {
            source = existingSource.get();
        } else {
            source = new GmailSource();
            source.setEmailAddress(emailAddress);
            source.setProvider("GOOGLE");
        }
        
        source.setCredential(encryptedRefreshToken);
        source.setStatus("ACTIVE");
        
        return gmailSourceRepository.save(source);
    }
}
