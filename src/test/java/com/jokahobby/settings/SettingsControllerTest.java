package com.jokahobby.settings;

import com.jokahobby.WithAccount;
import com.jokahobby.account.AccountRepository;
import com.jokahobby.account.AccountService;
import com.jokahobby.account.SignUpForm;
import com.jokahobby.domain.Account;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import static com.jokahobby.settings.SettingsController.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.context.support.TestExecutionEvent.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;


    @AfterEach
    void afterEach(){
        accountRepository.deleteAll();
    }

    @WithAccount("cutedog")
    @DisplayName("Profile update form")
    @Test
    void updateProfileForm() throws Exception{
        String bio = "This is a test bio";
        mockMvc.perform(get(SETTINGS_PROFILE_URL))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"));

    }


    @WithAccount("cutedog")
    @DisplayName("Profile update - correct data")
    @Test
    void updateProfile() throws Exception{
        String bio = "This is a test bio";
        mockMvc.perform(post(SETTINGS_PROFILE_URL)
                .param("bio", bio)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SETTINGS_PROFILE_URL))
                .andExpect(flash().attributeExists("message"));

        Account cutedog = accountRepository.findByNickname("cutedog");
        assertThat(cutedog.getBio()).isEqualTo(bio);
    }

    @WithAccount("cutedog")
    @DisplayName("Profile update - incorrect data")
    @Test
    void updateProfile_error() throws Exception{
        String bio = "This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bio\"This is a test bio\"This is a test bio\"This is a test bio\"";
        mockMvc.perform(post(SETTINGS_PROFILE_URL)
                        .param("bio", bio)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SETTINGS_PROFILE_VIEW))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(model().hasErrors());

        Account cutedog = accountRepository.findByNickname("cutedog");
        assertThat(cutedog.getBio()).isNull();
    }

    @WithAccount("cutedog")
    @DisplayName("Password update form")
    @Test
    void passwordUpdateForm() throws Exception{
        mockMvc.perform(get(SETTINGS_PASSWORD_URL))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"));
    }

    @WithAccount("cutedog")
    @DisplayName("Password update - correct data")
    @Test
    void passwordUpdate() throws Exception {
        String newPassword = "newPassword";
        mockMvc.perform(post(SETTINGS_PASSWORD_URL)
                        .param("newPassword", newPassword)
                        .param("newPasswordConfirm", newPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SETTINGS_PASSWORD_URL))
                .andExpect(flash().attributeExists("message"));

        Account cutedog = accountRepository.findByNickname("cutedog");
        assertThat(passwordEncoder.matches(newPassword, cutedog.getPassword())).isTrue();
    }

    @WithAccount("cutedog")
    @DisplayName("Password update - incorrect data")
    @Test
    void passwordUpdate_error() throws Exception {
        String newPassword = "newPassword";
        mockMvc.perform(post(SETTINGS_PASSWORD_URL)
                        .param("newPassword", newPassword)
                        .param("newPasswordConfirm", "wrongPassword")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SETTINGS_PASSWORD_VIEW))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"))
                .andExpect(model().hasErrors());

        Account cutedog = accountRepository.findByNickname("cutedog");
        assertThat(passwordEncoder.matches(newPassword, cutedog.getPassword())).isFalse();
    }

    @WithAccount("cutedog")
    @DisplayName("Nickname update form")
    @Test
    void nicknameUpdateForm() throws Exception{
        mockMvc.perform(get(SETTINGS_ACCOUNT_URL))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("nicknameForm"));
    }

    @WithAccount("cutedog")
    @DisplayName("Nickname update - correct data")
    @Test
    void nicknameUpdate() throws Exception{
        String newNickname = "newNickname";
        mockMvc.perform(post(SETTINGS_ACCOUNT_URL)
                        .param("nickname", newNickname)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(SETTINGS_ACCOUNT_URL))
                .andExpect(flash().attributeExists("message"));

        Account cutedog = accountRepository.findByNickname(newNickname);
        assertThat(cutedog.getNickname()).isEqualTo(newNickname);
    }

    @WithAccount("cutedog")
    @DisplayName("Nickname update - incorrect data")
    @Test
    void nicknameUpdate_error() throws Exception{
        String newNickname = "a";
        mockMvc.perform(post(SETTINGS_ACCOUNT_URL)
                        .param("nickname", newNickname)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SETTINGS_ACCOUNT_VIEW))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("nicknameForm"))
                .andExpect(model().hasErrors());

        assertThat(accountRepository.findByNickname(newNickname)).isNull();
    }




}