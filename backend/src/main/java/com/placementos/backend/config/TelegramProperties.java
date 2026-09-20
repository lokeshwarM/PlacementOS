package com.placementos.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    /**
     * Secret bot token obtained from @BotFather.
     * Must never be logged or exposed to clients.
     */
    private String botToken;

    /**
     * Username of the bot (e.g. PlacementOS_bot).
     * Used to construct deep links: https://t.me/<botUsername>?start=<token>.
     */
    private String botUsername;

    /**
     * Base URL for the Telegram Bot API. Default: https://api.telegram.org.
     */
    private String apiBaseUrl = "https://api.telegram.org";

    /**
     * Secret token used to validate incoming webhooks from Telegram (X-Telegram-Bot-Api-Secret-Token).
     */
    private String webhookSecret;

    public String getBotToken() { return botToken; }
    public void setBotToken(String botToken) { this.botToken = botToken; }

    public String getBotUsername() { return botUsername; }
    public void setBotUsername(String botUsername) { this.botUsername = botUsername; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public boolean isConfigured() {
        return botToken != null && !botToken.isBlank() && !botToken.equalsIgnoreCase("mock");
    }
}
