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

}