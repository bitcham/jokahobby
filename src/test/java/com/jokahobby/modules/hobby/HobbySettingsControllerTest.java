package com.jokahobby.modules.hobby;


import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountFactory;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.account.WithAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@MockMvcTest
class HobbySettingsControllerTest extends AbstractContainerBaseTest {

    @Autowired MockMvc mockMvc;
    @Autowired HobbyFactory hobbyFactory;
    @Autowired
    AccountFactory accountFactory;
    @Autowired
    AccountRepository accountRepository;
    @Autowired HobbyRepository hobbyRepository;

    @Test
    @WithAccount("cutedog")
    @DisplayName("View study description edit form - Failed (Unauthorized user)")
    void updateDescriptionForm_fail() throws Exception {
        Account cham = accountFactory.createAccount("cham");
        Hobby hobby = hobbyFactory.createHobby("test-hobby", cham);

        mockMvc.perform(get("/hobby/" + hobby.getPath() + "/settings/description"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAccount("cutedog")
    @DisplayName("View study description edit form - Success")
    void updateDescriptionForm_success() throws Exception {
        Account cutedog = accountRepository.findByNickname("cutedog");
        Hobby hobby = hobbyFactory.createHobby("test-hobby", cutedog);

        mockMvc.perform(get("/hobby/" + hobby.getPath() + "/settings/description"))
                .andExpect(status().isOk())
                .andExpect(view().name("hobby/settings/description"))
                .andExpect(model().attributeExists("hobbyDescriptionForm"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("hobby"));
    }

    @Test
    @WithAccount("cutedog")
    @DisplayName("Update study description - Success")
    void updateDescription_success() throws Exception {
        Account cutedog = accountRepository.findByNickname("cutedog");
        Hobby hobby = hobbyFactory.createHobby("test-hobby", cutedog);

        String settingsDescriptionUrl = "/hobby/" + hobby.getPath() + "/settings/description";
        mockMvc.perform(post(settingsDescriptionUrl)
                .param("shortDescription", "short description")
                .param("fullDescription", "full description")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(settingsDescriptionUrl))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    @WithAccount("cutedog")
    @DisplayName("Update study description - Failed")
    void updateDescription_fail() throws Exception {
        Account cutedog = accountRepository.findByNickname("cutedog");
        Hobby hobby = hobbyFactory.createHobby("test-hobby", cutedog);

        String settingsDescriptionUrl = "/hobby/" + hobby.getPath() + "/settings/description";
        mockMvc.perform(post(settingsDescriptionUrl)
                .param("shortDescription", "")
                .param("fullDescription", "full description")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("hobbyDescriptionForm"))
                .andExpect(model().attributeExists("hobby"))
                .andExpect(model().attributeExists("account"));
    }

}
