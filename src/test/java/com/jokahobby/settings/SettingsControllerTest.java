package com.jokahobby.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jokahobby.WithAccount;
import com.jokahobby.account.AccountRepository;
import com.jokahobby.account.AccountService;
import com.jokahobby.domain.Account;
import com.jokahobby.domain.Tag;
import com.jokahobby.domain.Zone;
import com.jokahobby.tag.TagForm;
import com.jokahobby.zone.ZoneForm;
import com.jokahobby.tag.TagRepository;
import com.jokahobby.zone.ZoneRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import java.util.Optional;

import static com.jokahobby.settings.SettingsController.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
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
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private ZoneRepository zoneRepository;

    private Zone testZone = Zone.builder().country("testCountry").
            city("testCity").localNameOfCity("테스트시").province("testProvince").build();

    @BeforeEach
    void beforeEach() {
        zoneRepository.save(testZone);
    }

    @AfterEach
    void afterEach(){
        accountRepository.deleteAll();
    }

    @WithAccount("cutedog")
    @DisplayName("Profile update form")
    @Test
    void updateProfileForm() throws Exception{
        String bio = "This is a test bio";
        mockMvc.perform(get(ROOT + SETTINGS + PROFILE))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("profile"));

    }


    @WithAccount("cutedog")
    @DisplayName("Profile update - correct data")
    @Test
    void updateProfile() throws Exception{
        String bio = "This is a test bio";
        mockMvc.perform(post(ROOT + SETTINGS + PROFILE)
                .param("bio", bio)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ROOT + SETTINGS + PROFILE))
                .andExpect(flash().attributeExists("message"));

        Account cutedog = accountRepository.findByNickname("cutedog");
        assertThat(cutedog.getBio()).isEqualTo(bio);
    }

    @WithAccount("cutedog")
    @DisplayName("Profile update - incorrect data")
    @Test
    void updateProfile_error() throws Exception{
        String bio = "This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bioThis is a test bio\"This is a test bio\"This is a test bio\"This is a test bio\"This is a test bio\"";
        mockMvc.perform(post(ROOT + SETTINGS + PROFILE)
                        .param("bio", bio)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SETTINGS + PROFILE))
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
        mockMvc.perform(get(ROOT + SETTINGS + PASSWORD))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("passwordForm"));
    }

    @WithAccount("cutedog")
    @DisplayName("Password update - correct data")
    @Test
    void passwordUpdate() throws Exception {
        String newPassword = "newPassword";
        mockMvc.perform(post(ROOT + SETTINGS + PASSWORD)
                        .param("newPassword", newPassword)
                        .param("newPasswordConfirm", newPassword)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ROOT + SETTINGS + PASSWORD))
                .andExpect(flash().attributeExists("message"));

        Account cutedog = accountRepository.findByNickname("cutedog");
        assertThat(passwordEncoder.matches(newPassword, cutedog.getPassword())).isTrue();
    }

    @WithAccount("cutedog")
    @DisplayName("Password update - incorrect data")
    @Test
    void passwordUpdate_error() throws Exception {
        String newPassword = "newPassword";
        mockMvc.perform(post(ROOT + SETTINGS + PASSWORD)
                        .param("newPassword", newPassword)
                        .param("newPasswordConfirm", "wrongPassword")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name( SETTINGS + PASSWORD))
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
        mockMvc.perform(get(ROOT + SETTINGS + ACCOUNT))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("nicknameForm"));
    }

    @WithAccount("cutedog")
    @DisplayName("Nickname update - correct data")
    @Test
    void nicknameUpdate() throws Exception{
        String newNickname = "newNickname";
        mockMvc.perform(post(ROOT + SETTINGS + ACCOUNT)
                        .param("nickname", newNickname)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(ROOT + SETTINGS + ACCOUNT))
                .andExpect(flash().attributeExists("message"));

        Account cutedog = accountRepository.findByNickname(newNickname);
        assertThat(cutedog.getNickname()).isEqualTo(newNickname);
    }

    @WithAccount("cutedog")
    @DisplayName("Nickname update - incorrect data")
    @Test
    void nicknameUpdate_error() throws Exception{
        String newNickname = "a";
        mockMvc.perform(post(ROOT + SETTINGS + ACCOUNT)
                        .param("nickname", newNickname)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(SETTINGS + ACCOUNT))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("nicknameForm"))
                .andExpect(model().hasErrors());

        assertThat(accountRepository.findByNickname(newNickname)).isNull();
    }

    @WithAccount("cutedog")
    @DisplayName("tag update form")
    @Test
    void tagUpdateForm() throws Exception{
        mockMvc.perform(get(ROOT + SETTINGS + TAGS))
                .andExpect(view().name(SETTINGS + TAGS))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("chamlist"))
                .andExpect(model().attributeExists("tags"));
    }

    @WithAccount("cutedog")
    @DisplayName("add tag to account")
    @Test
    void addTag() throws Exception{
        TagForm tagForm = new TagForm();
        tagForm.setTagTitle("newTag");

        mockMvc.perform(post(ROOT + SETTINGS + TAGS + "/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tagForm))
                .with(csrf()))
                .andExpect(status().isOk());

        Optional<Tag> newTag = tagRepository.findByTitle("newTag");
        assertThat(newTag).isPresent();
        assertThat(accountRepository.findByNickname("cutedog").getTags()).contains(newTag.get());
    }

    @WithAccount("cutedog")
    @DisplayName("remove tag from account")
    @Test
    void removeTag() throws Exception{
        Tag newTag = Tag.builder().title("newTag").build();
        tagRepository.save(newTag);
        Account cutedog = accountRepository.findByNickname("cutedog");
        accountService.addTag(cutedog, newTag);

        TagForm tagForm = new TagForm();
        tagForm.setTagTitle("newTag");

        mockMvc.perform(post(ROOT + SETTINGS + TAGS + "/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tagForm))
                .with(csrf()))
                .andExpect(status().isOk());

        assertThat(accountRepository.findByNickname("cutedog").getTags()).doesNotContain(newTag);
    }

    @WithAccount("cutedog")
    @DisplayName("zone update form")
    @Test
    void updateZonesForm() throws Exception {
        mockMvc.perform(get(ROOT + SETTINGS + ZONES))
                .andExpect(view().name(SETTINGS + ZONES))
                .andExpect(model().attributeExists("account"))
                .andExpect(model().attributeExists("chamlist"))
                .andExpect(model().attributeExists("zones"));
    }

    @WithAccount("cutedog")
    @DisplayName("add zone to account")
    @Test
    void addZone() throws Exception {
        ZoneForm zoneForm = new ZoneForm();
        zoneForm.setZoneName(testZone.toString());

        mockMvc.perform(post(ROOT + SETTINGS + ZONES + "/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneForm))
                        .with(csrf()))
                .andExpect(status().isOk());

        Account cutedog = accountRepository.findByNickname("cutedog");
        Optional<Zone> zone = zoneRepository.findByCityAndProvince(testZone.getCity(), testZone.getProvince());
        assertTrue(cutedog.getZones().contains(zone.get()));
    }

    @WithAccount("cutedog")
    @DisplayName("remove zone from account")
    @Test
    void removeZone() throws Exception {
        Account cutedog = accountRepository.findByNickname("cutedog");
        Optional<Zone> zone = zoneRepository.findByCityAndProvince(testZone.getCity(), testZone.getProvince());
        accountService.addZone(cutedog, zone.get());

        ZoneForm zoneForm = new ZoneForm();
        zoneForm.setZoneName(testZone.toString());

        mockMvc.perform(post(ROOT + SETTINGS + ZONES + "/remove")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zoneForm))
                        .with(csrf()))
                .andExpect(status().isOk());

        assertFalse(cutedog.getZones().contains(zone.get()));
    }




}