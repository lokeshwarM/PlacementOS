package com.placementos.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActuatorSecurityTest {

    @LocalServerPort
    private int port;

    @Test
    void actuatorHealth_returnsUp_withoutSensitiveDetails() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/actuator/health"))
                .GET()
                .build();
                
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        assertThat(response.statusCode()).isEqualTo(200);
        
        String body = response.body();
        assertThat(body).isNotNull();
        assertThat(body).contains("\"status\":\"UP\"");
        
        // Assert sensitive details are hidden
        assertThat(body).doesNotContain("\"components\":");
        assertThat(body).doesNotContain("\"db\":");
    }
}
