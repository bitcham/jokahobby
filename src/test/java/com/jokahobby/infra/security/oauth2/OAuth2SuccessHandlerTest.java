package com.jokahobby.infra.security.oauth2;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.infra.config.AppProperties;
import com.jokahobby.infra.security.jwt.JwtProvider;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class OAuth2SuccessHandlerTest extends AbstractContainerBaseTest {

    @Autowired private OAuth2SuccessHandler oAuth2SuccessHandler;
    @Autowired private AccountRepository accountRepository;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private AppProperties appProperties;

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }

    @DisplayName("Success handler generates tokens and redirects with nicknameRequired=true for new account")
    @Test
    void successHandler_newAccount() throws Exception {
        Account account = Account.builder()
                .provider("GOOGLE")
                .providerId("handler-test-sub")
                .email("handler@gmail.com")
                .joinedAt(Instant.now())
                .build();
        accountRepository.save(account);

        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(account, Map.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "TestAgent");
        MockHttpServletResponse response = new MockHttpServletResponse();

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        String redirectUrl = response.getRedirectedUrl();
        assertThat(redirectUrl).startsWith(appProperties.getFrontendUrl() + "/oauth2/callback");
        assertThat(redirectUrl).contains("token=");
        assertThat(redirectUrl).contains("expiresIn=");
        assertThat(redirectUrl).contains("nicknameRequired=true");

        // Verify refresh token cookie is set
        assertThat(response.getHeader("Set-Cookie")).contains("refreshToken");
    }

    @DisplayName("Success handler sets nicknameRequired=false for account with nickname")
    @Test
    void successHandler_existingAccountWithNickname() throws Exception {
        Account account = Account.builder()
                .provider("GOOGLE")
                .providerId("handler-test-sub-2")
                .email("existing@gmail.com")
                .nickname("existinguser")
                .joinedAt(Instant.now())
                .build();
        accountRepository.save(account);

        OAuth2UserPrincipal principal = new OAuth2UserPrincipal(account, Map.of());
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        oAuth2SuccessHandler.onAuthenticationSuccess(request, response, auth);

        String redirectUrl = response.getRedirectedUrl();
        assertThat(redirectUrl).contains("nicknameRequired=false");
    }
}
