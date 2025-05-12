package com.jokahobby.hobby;

import com.jokahobby.WithAccount;
import com.jokahobby.account.AccountRepository;
import com.jokahobby.domain.Account;
import com.jokahobby.domain.Hobby;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@RequiredArgsConstructor
public class HobbyControllerTest {
    
    @Autowired
    HobbyService hobbyService;
    @Autowired
    HobbyRepository hobbyRepository;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    AccountRepository accountRepository;

    @AfterEach
    void afterEach() {
        accountRepository.deleteAll();
    }

    @Test
    @WithAccount("cutedog")
    @DisplayName("Hobby create form")
    void createhobbyForm() throws Exception {
        mockMvc.perform(get("/new-hobby"))
                .andExpect(status().isOk())
                .andExpect(view().name("hobby/form"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("hobbyForm"));
    }

    @Test
    @WithAccount("cutedog")
    @DisplayName("hobby create - success")
    void createhobby_success() throws Exception {
        mockMvc.perform(post("/new-hobby")
                        .param("path", "test-path")
                        .param("title", "hobby title")
                        .param("shortDescription", "short description of a hobby")
                        .param("fullDescription", "full description of a hobby")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hobby/test-path"));

        Hobby hobby = hobbyRepository.findByPath("test-path");
        assertNotNull(hobby);
        Account account = accountRepository.findByNickname("cutedog");
        assertTrue(hobby.getManagers().contains(account));
    }

    @Test
    @WithAccount("cutedog")
    @DisplayName("hobby create - fail")
    void createhobby_fail() throws Exception {
        mockMvc.perform(post("/new-hobby")
                        .param("path", "wrong path")
                        .param("title", "hobby title")
                        .param("shortDescription", "short description of a hobby")
                        .param("fullDescription", "full description of a hobby")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("hobby/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeExists("hobbyForm"))
                .andExpect(model().attributeExists("account"));

        Hobby hobby = hobbyRepository.findByPath("test-path");
        assertNull(hobby);
    }

    @Test
    @WithAccount("cutedog")
    @DisplayName("Hobby search")
    void viewhobby() throws Exception {
        Hobby hobby = new Hobby();
        hobby.setPath("test-path");
        hobby.setTitle("test hobby");
        hobby.setShortDescription("short description");
        hobby.setFullDescription("<p>full description</p>");

        Account cutedog = accountRepository.findByNickname("cutedog");
        hobbyService.createNewHobby(hobby, cutedog);

        mockMvc.perform(get("/hobby/test-path"))
                .andExpect(view().name("hobby/view"))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("hobby"));
    }
}
