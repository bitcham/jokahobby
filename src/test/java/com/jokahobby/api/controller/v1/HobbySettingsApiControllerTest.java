package com.jokahobby.api.controller.v1;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.infra.security.jwt.JwtProvider;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.hobby.*;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.tag.TagRepository;
import com.jokahobby.modules.zone.Zone;
import com.jokahobby.modules.zone.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class HobbySettingsApiControllerTest extends AbstractContainerBaseTest {

    @Autowired MockMvcTester mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired HobbyRepository hobbyRepository;
    @Autowired HobbyManagerRepository hobbyManagerRepository;
    @Autowired HobbyMemberRepository hobbyMemberRepository;
    @Autowired HobbyTagRepository hobbyTagRepository;
    @Autowired HobbyZoneRepository hobbyZoneRepository;
    @Autowired TagRepository tagRepository;
    @Autowired ZoneRepository zoneRepository;
    @Autowired JwtProvider jwtProvider;

    private Account managerAccount;
    private Account otherAccount;
    private Hobby testHobby;
    private String managerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        managerAccount = accountRepository.save(Account.builder()
                .email("manager@example.com")
                .nickname("manager")
                .provider("google")
                .providerId("google-manager")
                .joinedAt(Instant.now())
                .build());

        otherAccount = accountRepository.save(Account.builder()
                .email("other@example.com")
                .nickname("otheruser")
                .provider("google")
                .providerId("google-other")
                .joinedAt(Instant.now())
                .build());

        testHobby = hobbyRepository.save(Hobby.builder()
                .path("test-hobby")
                .title("Test Hobby")
                .shortDescription("Short description")
                .fullDescription("Full description")
                .build());

        hobbyManagerRepository.save(HobbyManager.builder()
                .hobby(testHobby).account(managerAccount).build());

        managerToken = "Bearer " + jwtProvider.createAccessToken(managerAccount.getId());
        otherToken = "Bearer " + jwtProvider.createAccessToken(otherAccount.getId());
    }

    // ===== GET /api/v1/hobbies/{path}/settings =====

    @Nested
    @DisplayName("GET /api/v1/hobbies/{path}/settings")
    class GetSettings {

        @Test
        @DisplayName("returns full settings for manager")
        void managerAccess() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/settings", "test-hobby")
                            .header("Authorization", managerToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.title").isEqualTo("Test Hobby");
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManagerAccess() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/settings", "test-hobby")
                            .header("Authorization", otherToken))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/settings", "test-hobby"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== PUT /api/v1/hobbies/{path}/settings/description =====

    @Nested
    @DisplayName("PUT /api/v1/hobbies/{path}/settings/description")
    class UpdateDescription {

        @Test
        @DisplayName("updates description with valid data")
        void validUpdate() {
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/description", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "shortDescription": "Updated short",
                                        "fullDescription": "Updated full description"
                                    }
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 for invalid data")
        void invalidData() {
            String longDesc = "a".repeat(151);
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/description", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"shortDescription": "%s", "fullDescription": "Valid"}
                                    """.formatted(longDesc)))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/description", "test-hobby")
                            .header("Authorization", otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"shortDescription": "Short", "fullDescription": "Full"}
                                    """))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== PUT /api/v1/hobbies/{path}/settings/banner =====

    @Nested
    @DisplayName("PUT /api/v1/hobbies/{path}/settings/banner")
    class UpdateBanner {

        @Test
        @DisplayName("updates banner image")
        void validUpdate() {
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/banner", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"image": "data:image/png;base64,abc123"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 for blank image")
        void blankImage() {
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/banner", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"image": ""}
                                    """))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/banner", "test-hobby")
                            .header("Authorization", otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"image": "data:image/png;base64,abc123"}
                                    """))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== POST /api/v1/hobbies/{path}/settings/banner/enable & disable =====

    @Nested
    @DisplayName("Banner enable/disable")
    class BannerToggle {

        @Test
        @DisplayName("enables banner")
        void enable() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/banner/enable", "test-hobby")
                            .header("Authorization", managerToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("disables banner")
        void disable() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/banner/disable", "test-hobby")
                            .header("Authorization", managerToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/banner/enable", "test-hobby")
                            .header("Authorization", otherToken))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== Tags management =====

    @Nested
    @DisplayName("Tags management")
    class TagsManagement {

        @Test
        @DisplayName("returns tag list")
        void getTags() {
            Tag tag = tagRepository.save(Tag.builder().title("spring").build());
            hobbyTagRepository.save(HobbyTag.builder().hobby(testHobby).tag(tag).build());

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/settings/tags", "test-hobby")
                            .header("Authorization", managerToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data[0].title").isEqualTo("spring");
        }

        @Test
        @DisplayName("adds valid tag")
        void addTag() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/tags", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tagTitle": "java"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("removes existing tag")
        void removeTag() {
            Tag tag = tagRepository.save(Tag.builder().title("remove-me").build());
            hobbyTagRepository.save(HobbyTag.builder().hobby(testHobby).tag(tag).build());

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/settings/tags", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tagTitle": "remove-me"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/settings/tags", "test-hobby")
                            .header("Authorization", otherToken))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== Zones management =====

    @Nested
    @DisplayName("Zones management")
    class ZonesManagement {

        private Zone testZone;

        @BeforeEach
        void setUpZone() {
            testZone = zoneRepository.findByCityAndProvince("Seoul", "none")
                    .orElseGet(() -> zoneRepository.save(Zone.builder()
                            .country("Korea").city("Seoul")
                            .localNameOfCity("서울").province("none").build()));
        }

        @Test
        @DisplayName("returns zone list")
        void getZones() {
            hobbyZoneRepository.save(HobbyZone.builder().hobby(testHobby).zone(testZone).build());

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/settings/zones", "test-hobby")
                            .header("Authorization", managerToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data[0].city").isEqualTo("Seoul");
        }

        @Test
        @DisplayName("adds valid zone")
        void addZone() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/zones", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"zoneName": "Korea/Seoul"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("removes existing zone")
        void removeZone() {
            hobbyZoneRepository.save(HobbyZone.builder().hobby(testHobby).zone(testZone).build());

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/settings/zones", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"zoneName": "Korea/Seoul"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/settings/zones", "test-hobby")
                            .header("Authorization", otherToken))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== Publish & Close =====

    @Nested
    @DisplayName("Publish and Close")
    class PublishAndClose {

        @Test
        @DisplayName("publishes unpublished hobby")
        void publish() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/publish", "test-hobby")
                            .header("Authorization", managerToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("closes published hobby")
        void close() {
            testHobby.publish();
            hobbyRepository.save(testHobby);

            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/close", "test-hobby")
                            .header("Authorization", managerToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns error for invalid state transition")
        void invalidTransition() {
            testHobby.publish();
            hobbyRepository.save(testHobby);

            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/publish", "test-hobby")
                            .header("Authorization", managerToken))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("HOBBY_001");
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/publish", "test-hobby")
                            .header("Authorization", otherToken))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== Recruit =====

    @Nested
    @DisplayName("Recruit start/stop")
    class Recruit {

        @BeforeEach
        void publishHobby() {
            testHobby.publish();
            hobbyRepository.save(testHobby);
        }

        @Test
        @DisplayName("starts recruiting")
        void startRecruit() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/recruit/start", "test-hobby")
                            .header("Authorization", managerToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("stops recruiting")
        void stopRecruit() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/recruit/stop", "test-hobby")
                            .header("Authorization", managerToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/settings/recruit/start", "test-hobby")
                            .header("Authorization", otherToken))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== Update Path =====

    @Nested
    @DisplayName("PUT /api/v1/hobbies/{path}/settings/path")
    class UpdatePath {

        @Test
        @DisplayName("updates path with valid value")
        void validUpdate() {
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/path", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPath": "new-path"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns error for invalid path format")
        void invalidPath() {
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/path", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPath": "a"}
                                    """))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns error for duplicate path")
        void duplicatePath() {
            hobbyRepository.save(Hobby.builder()
                    .path("existing-path")
                    .title("Existing")
                    .shortDescription("Short")
                    .fullDescription("Full")
                    .build());

            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/path", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPath": "existing-path"}
                                    """))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("HOBBY_007");
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/path", "test-hobby")
                            .header("Authorization", otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newPath": "new-path"}
                                    """))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== Update Title =====

    @Nested
    @DisplayName("PUT /api/v1/hobbies/{path}/settings/title")
    class UpdateTitle {

        @Test
        @DisplayName("updates title with valid value")
        void validUpdate() {
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/title", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newTitle": "Updated Title"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns error for duplicate title")
        void duplicateTitle() {
            hobbyRepository.save(Hobby.builder()
                    .path("other-hobby")
                    .title("Existing Title")
                    .shortDescription("Short")
                    .fullDescription("Full")
                    .build());

            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/title", "test-hobby")
                            .header("Authorization", managerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newTitle": "Existing Title"}
                                    """))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("HOBBY_008");
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/settings/title", "test-hobby")
                            .header("Authorization", otherToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"newTitle": "New Title"}
                                    """))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }
}
