package com.jokahobby.infra.security.oauth2;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockMvcTest
class OAuth2SessionTest extends AbstractContainerBaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("OAuth2 authorization endpoint creates session despite STATELESS policy")
    void oAuth2AuthorizationEndpoint_createsSessionDespiteStateless() throws Exception {
        MvcResult result = mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Check if a session was created during the OAuth2 authorization request
        boolean sessionCreated = result.getRequest().getSession(false) != null;

        String redirectUrl = result.getResponse().getRedirectedUrl();
        System.out.println("=== Redirect Location ===");
        System.out.println(redirectUrl);
        System.out.println("=== Session Created ===");
        System.out.println("Session created despite STATELESS: " + sessionCreated);

        // If session exists, the OAuth2 state parameter was stored in the session
        // This proves STATELESS config still relies on session for OAuth2 state storage
        assertThat(sessionCreated)
                .as("STATELESS policy should NOT create a session, but OAuth2 state storage forces it")
                .isTrue();

        // Verify the redirect URL contains the state parameter (stored in session)
        assertThat(redirectUrl).contains("state=");
    }
}
