package com.placementos.backend.domain.service.provider;

import tools.jackson.databind.ObjectMapper;
import com.placementos.backend.config.TelegramProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TelegramNotificationProviderTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private TelegramProperties properties;
    private ObjectMapper objectMapper;
    private TelegramNotificationProvider provider;

    @BeforeEach
    void setUp() {
        properties = new TelegramProperties();
        properties.setBotToken("123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11");
        properties.setBotUsername("PlacementOS_bot");
        properties.setApiBaseUrl("https://api.telegram.org");

        objectMapper = new ObjectMapper();
        provider = new TelegramNotificationProvider(properties, objectMapper, httpClient);
    }

    @Test
    void sendTelegramMessage_unlinkedOrBlankRecipient_returnsPermanentFailure() {
        NotificationDeliveryResult nullRes = provider.sendTelegramMessage(null, "Test", "key-1");
        assertFalse(nullRes.success());
        assertFalse(nullRes.retryable());

        NotificationDeliveryResult blankRes = provider.sendTelegramMessage("", "Test", "key-2");
        assertFalse(blankRes.success());
        assertFalse(blankRes.retryable());

        NotificationDeliveryResult unlinkedRes = provider.sendTelegramMessage("UNLINKED", "Test", "key-3");
        assertFalse(unlinkedRes.success());
        assertFalse(unlinkedRes.retryable());
    }

    @Test
    void sendTelegramMessage_unconfigured_returnsMockSuccess() {
        properties.setBotToken(""); // unconfigured
        NotificationDeliveryResult result = provider.sendTelegramMessage("987654321", "Test", "key-4");

        assertTrue(result.success());
        assertNotNull(result.externalMessageId());
        assertTrue(result.externalMessageId().startsWith("mock-tg-"));
        verifyNoInteractions(httpClient);
    }

    @Test
    void sendTelegramMessage_success_parsesMessageId() throws Exception {
        String jsonResponse = """
                {
                    "ok": true,
                    "result": {
                        "message_id": 999123,
                        "chat": { "id": 987654321 },
                        "date": 1726000000,
                        "text": "Hello"
                    }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        NotificationDeliveryResult result = provider.sendTelegramMessage("987654321", "Hello", "key-5");

        assertTrue(result.success());
        assertEquals("999123", result.externalMessageId());
        assertNull(result.errorMessage());
        assertFalse(result.retryable());
    }

    @Test
    void sendTelegramMessage_rateLimited_returnsRetryableFailure() throws Exception {
        String jsonResponse = """
                {
                    "ok": false,
                    "error_code": 429,
                    "description": "Too Many Requests: retry after 8",
                    "parameters": {
                        "retry_after": 8
                    }
                }
                """;

        when(httpResponse.statusCode()).thenReturn(429);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        NotificationDeliveryResult result = provider.sendTelegramMessage("987654321", "Hello", "key-6");

        assertFalse(result.success());
        assertTrue(result.retryable());
        assertTrue(result.errorMessage().contains("retry after 8"));
    }

    @Test
    void sendTelegramMessage_userBlocked_returnsPermanentFailure() throws Exception {
        String jsonResponse = """
                {
                    "ok": false,
                    "error_code": 403,
                    "description": "Forbidden: bot was blocked by the user"
                }
                """;

        when(httpResponse.statusCode()).thenReturn(403);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        NotificationDeliveryResult result = provider.sendTelegramMessage("987654321", "Hello", "key-7");

        assertFalse(result.success());
        assertFalse(result.retryable());
        assertTrue(result.errorMessage().contains("bot was blocked by the user"));
    }

    @Test
    void sendTelegramMessage_invalidToken_returnsPermanentFailure() throws Exception {
        String jsonResponse = """
                {
                    "ok": false,
                    "error_code": 401,
                    "description": "Unauthorized"
                }
                """;

        when(httpResponse.statusCode()).thenReturn(401);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        NotificationDeliveryResult result = provider.sendTelegramMessage("987654321", "Hello", "key-8");

        assertFalse(result.success());
        assertFalse(result.retryable());
        assertTrue(result.errorMessage().contains("invalid or unauthorized"));
    }

    @Test
    void sendTelegramMessage_httpTimeout_returnsRetryableFailure() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("Connection timed out"));

        NotificationDeliveryResult result = provider.sendTelegramMessage("987654321", "Hello", "key-9");

        assertFalse(result.success());
        assertTrue(result.retryable());
        assertTrue(result.errorMessage().contains("timed out"));
    }

    @Test
    void sendTelegramMessage_neverLogsBotToken() throws Exception {
        // Assert token is not contained in any delivery result error string
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: chat not found\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        NotificationDeliveryResult result = provider.sendTelegramMessage("987654321", "Hello", "key-10");

        assertFalse(result.errorMessage().contains(properties.getBotToken()));
    }
}
