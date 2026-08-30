package com.placementos.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpoint_shouldBePermitted() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void otherActuatorEndpoints_shouldBeDenied() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiV1Endpoints_shouldBeTemporarilyPermitted() throws Exception {
        // This validates the temporary "permitAll()" for /api/v1/** in this milestone
        mockMvc.perform(get("/api/v1/placements"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownEndpoints_shouldBeAuthenticated() throws Exception {
        // Fallback catch-all should require authentication (403 or 401 depending on exception handling setup)
        // Spring Security defaults to 401 when no auth is provided, but depending on the filter chain it might be 403.
        // It should NOT be 404 since security intercepts before routing. 
        // Actually, in Spring Boot 3+, unauthorized requests to unmapped endpoints might return 401.
        mockMvc.perform(get("/api/v2/secret"))
                .andExpect(status().isForbidden())
                .andReturn();
    }
}
