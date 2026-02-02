package com.jokahobby.modules.account;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockMvcTest
class AccountControllerTest extends AbstractContainerBaseTest {

    @Autowired private MockMvc mockMvc;

    @DisplayName("OAuth2 authorization endpoint redirects to Google")
    @Test
    void oauth2AuthorizationEndpoint() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection());
    }

    @DisplayName("Signup endpoint no longer exists")
    @Test
    void signupEndpointRemoved() throws Exception {
        mockMvc.perform(get("/api/v1/auth/signup"))
                .andExpect(status().isUnauthorized());
    }

    @DisplayName("Login endpoint no longer exists")
    @Test
    void loginEndpointRemoved() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isUnauthorized());
    }
}
