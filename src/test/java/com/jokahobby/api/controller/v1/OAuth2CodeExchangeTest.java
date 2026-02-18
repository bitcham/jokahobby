package com.jokahobby.api.controller.v1;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.infra.security.jwt.TokenHashUtil;
import com.jokahobby.infra.security.oauth2.OAuth2AuthorizationCodeStore;
import com.jokahobby.infra.security.oauth2.OAuth2AuthorizationData;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class OAuth2CodeExchangeTest extends AbstractContainerBaseTest {

    @Autowired MockMvcTester mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired OAuth2AuthorizationCodeStore codeStore;

    private Account testAccountNoNickname;
    private Account testAccountWithNickname;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();

        testAccountNoNickname = accountRepository.save(Account.builder()
                .email("nonick@example.com")
                .provider("GOOGLE")
                .providerId("google-nonick-123")
                .joinedAt(Instant.now())
                .build());

        testAccountWithNickname = accountRepository.save(Account.builder()
                .email("withnick@example.com")
                .nickname("existinguser")
                .provider("GOOGLE")
                .providerId("google-withnick-456")
                .joinedAt(Instant.now())
                .build());
    }

    private String storeCode(Account account, String binding) {
        String bindingHash = TokenHashUtil.sha256(binding);
        boolean nicknameRequired = account.getNickname() == null;
        OAuth2AuthorizationData data = new OAuth2AuthorizationData(
                account.getId(), nicknameRequired, "TestAgent", "127.0.0.1",
                bindingHash, Instant.now());
        return codeStore.store(data);
    }

    private MvcTestResult exchangeCode(String code, String binding) {
        var builder = mockMvc.post().uri("/api/v1/auth/oauth2/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}");
        if (binding != null) {
            builder = builder.cookie(new Cookie("oauth2_binding", binding));
        }
        return builder.exchange();
    }

    @Test
    @DisplayName("Valid code + binding cookie returns 200 with tokens")
    void validCodeAndBinding_returns200() {
        String binding = "validbinding123";
        String code = storeCode(testAccountNoNickname, binding);

        MvcTestResult result = exchangeCode(code, binding);

        assertThat(result)
                .hasStatusOk()
                .headers().containsHeader("Set-Cookie");
        assertThat(result)
                .headers().hasValue("Cache-Control", "no-store");
        assertThat(result).bodyJson()
                .extractingPath("$.success").isEqualTo(true);
        assertThat(result).bodyJson()
                .extractingPath("$.data.accessToken").isNotNull();
        assertThat(result).bodyJson()
                .extractingPath("$.data.expiresIn").isNotNull();
        assertThat(result).bodyJson()
                .extractingPath("$.data.nicknameRequired").isEqualTo(true);
    }

    @Test
    @DisplayName("nicknameRequired=true for account without nickname")
    void nicknameRequired_true() {
        String binding = "binding1";
        String code = storeCode(testAccountNoNickname, binding);

        MvcTestResult result = exchangeCode(code, binding);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson()
                .extractingPath("$.data.nicknameRequired").isEqualTo(true);
    }

    @Test
    @DisplayName("nicknameRequired=false for account with nickname")
    void nicknameRequired_false() {
        String binding = "binding2";
        String code = storeCode(testAccountWithNickname, binding);

        MvcTestResult result = exchangeCode(code, binding);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson()
                .extractingPath("$.data.nicknameRequired").isEqualTo(false);
    }

    @Test
    @DisplayName("Already used code returns 401")
    void usedCode_returns401() {
        String binding = "binding3";
        String code = storeCode(testAccountNoNickname, binding);

        // First exchange succeeds
        exchangeCode(code, binding);

        // Second exchange fails
        MvcTestResult result = exchangeCode(code, binding);

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson()
                .extractingPath("$.error.code").isEqualTo("AUTH_008");
    }

    @Test
    @DisplayName("Unknown code returns 401")
    void unknownCode_returns401() {
        MvcTestResult result = exchangeCode(UUID.randomUUID().toString(), "anybinding");

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson()
                .extractingPath("$.error.code").isEqualTo("AUTH_008");
    }

    @Test
    @DisplayName("Blank code returns 400")
    void blankCode_returns400() {
        MvcTestResult result = exchangeCode("", "anybinding");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Missing binding cookie returns 401")
    void missingBindingCookie_returns401() {
        String binding = "binding5";
        String code = storeCode(testAccountNoNickname, binding);

        MvcTestResult result = exchangeCode(code, null);

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson()
                .extractingPath("$.error.code").isEqualTo("AUTH_008");
    }

    @Test
    @DisplayName("Wrong binding cookie returns 401")
    void wrongBindingCookie_returns401() {
        String binding = "correctbinding";
        String code = storeCode(testAccountNoNickname, binding);

        MvcTestResult result = exchangeCode(code, "wrongbinding");

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson()
                .extractingPath("$.error.code").isEqualTo("AUTH_008");
    }

    @Test
    @DisplayName("Wrong binding does not consume code, valid retry succeeds (DoS protection)")
    void wrongBinding_thenValidRetry_succeeds() {
        String correctBinding = "correctbinding";
        String code = storeCode(testAccountNoNickname, correctBinding);

        // Wrong binding attempt
        MvcTestResult wrongResult = exchangeCode(code, "wrongbinding");
        assertThat(wrongResult).hasStatus(HttpStatus.UNAUTHORIZED);

        // Valid retry with correct binding should succeed
        MvcTestResult validResult = exchangeCode(code, correctBinding);
        assertThat(validResult).hasStatusOk();
        assertThat(validResult).bodyJson()
                .extractingPath("$.data.accessToken").isNotNull();
    }
}
