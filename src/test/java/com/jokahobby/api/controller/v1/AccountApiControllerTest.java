package com.jokahobby.api.controller.v1;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.infra.security.jwt.JwtProvider;
import com.jokahobby.modules.account.*;
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
class AccountApiControllerTest extends AbstractContainerBaseTest {

    @Autowired MockMvcTester mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired AccountTagRepository accountTagRepository;
    @Autowired AccountZoneRepository accountZoneRepository;
    @Autowired TagRepository tagRepository;
    @Autowired ZoneRepository zoneRepository;
    @Autowired JwtProvider jwtProvider;

    private Account testAccount;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testAccount = accountRepository.save(Account.builder()
                .email("test@example.com")
                .nickname("testuser")
                .provider("google")
                .providerId("google-123")
                .joinedAt(Instant.now())
                .bio("Hello")
                .hobbyCreatedByWeb(true)
                .hobbyEnrollmentResultByWeb(true)
                .hobbyUpdatedByWeb(true)
                .build());
        accessToken = jwtProvider.createAccessToken(testAccount.getId());
    }

    private String bearer() {
        return "Bearer " + accessToken;
    }

    // ===== 1. GET /api/v1/accounts/{nickname} =====

    @Nested
    @DisplayName("GET /api/v1/accounts/{nickname}")
    class GetPublicProfile {

        @Test
        @DisplayName("returns profile for existing nickname")
        void existingNickname() {
            assertThat(mockMvc.get().uri("/api/v1/accounts/{nickname}", testAccount.getNickname()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            assertThat(mockMvc.get().uri("/api/v1/accounts/{nickname}", testAccount.getNickname()))
                    .bodyJson()
                    .extractingPath("$.data.nickname").isEqualTo("testuser");
        }

        @Test
        @DisplayName("returns 404 for non-existing nickname")
        void nonExistingNickname() {
            assertThat(mockMvc.get().uri("/api/v1/accounts/{nickname}", "unknown"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(false);

            assertThat(mockMvc.get().uri("/api/v1/accounts/{nickname}", "unknown"))
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("RESOURCE_001");
        }

        @Test
        @DisplayName("accessible without authentication")
        void noAuthRequired() {
            assertThat(mockMvc.get().uri("/api/v1/accounts/{nickname}", testAccount.getNickname()))
                    .hasStatusOk();
        }
    }

    // ===== 2. GET /api/v1/accounts/me =====

    @Nested
    @DisplayName("GET /api/v1/accounts/me")
    class GetMyAccount {

        @Test
        @DisplayName("returns full account info for authenticated user")
        void authenticated() {
            assertThat(mockMvc.get().uri("/api/v1/accounts/me")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.email").isEqualTo("test@example.com");

            assertThat(mockMvc.get().uri("/api/v1/accounts/me")
                            .header("Authorization", bearer()))
                    .bodyJson()
                    .extractingPath("$.data.nickname").isEqualTo("testuser");

            assertThat(mockMvc.get().uri("/api/v1/accounts/me")
                            .header("Authorization", bearer()))
                    .bodyJson()
                    .extractingPath("$.data.provider").isEqualTo("google");
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.get().uri("/api/v1/accounts/me"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== 3. PUT /api/v1/accounts/me/profile =====

    @Nested
    @DisplayName("PUT /api/v1/accounts/me/profile")
    class UpdateProfile {

        @Test
        @DisplayName("updates profile with valid data")
        void validUpdate() {
            assertThat(mockMvc.put().uri("/api/v1/accounts/me/profile")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "bio": "Updated bio",
                                        "url": "https://example.com",
                                        "location": "Seoul",
                                        "profileImage": null
                                    }
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.bio").isEqualTo("Updated bio");
        }

        @Test
        @DisplayName("returns 400 when bio exceeds 50 characters")
        void bioTooLong() {
            String longBio = "a".repeat(51);
            assertThat(mockMvc.put().uri("/api/v1/accounts/me/profile")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bio": "%s", "url": null, "location": null, "profileImage": null}
                                    """.formatted(longBio)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(false);
        }

        @Test
        @DisplayName("returns 400 when url exceeds 50 characters")
        void urlTooLong() {
            String longUrl = "https://" + "a".repeat(50);
            assertThat(mockMvc.put().uri("/api/v1/accounts/me/profile")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bio": null, "url": "%s", "location": null, "profileImage": null}
                                    """.formatted(longUrl)))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(false);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.put().uri("/api/v1/accounts/me/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bio": "test", "url": null, "location": null, "profileImage": null}
                                    """))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== 4. PUT /api/v1/accounts/me/notifications =====

    @Nested
    @DisplayName("PUT /api/v1/accounts/me/notifications")
    class UpdateNotifications {

        @Test
        @DisplayName("updates notification settings with valid data")
        void validUpdate() {
            assertThat(mockMvc.put().uri("/api/v1/accounts/me/notifications")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "hobbyCreatedByEmail": true,
                                        "hobbyCreatedByWeb": false,
                                        "hobbyEnrollmentResultByEmail": true,
                                        "hobbyEnrollmentResultByWeb": false,
                                        "hobbyUpdatedByEmail": true,
                                        "hobbyUpdatedByWeb": false
                                    }
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.put().uri("/api/v1/accounts/me/notifications")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== 5. PUT /api/v1/accounts/me/nickname =====

    @Nested
    @DisplayName("PUT /api/v1/accounts/me/nickname")
    class UpdateNickname {

        @Test
        @DisplayName("updates nickname with valid value")
        void validUpdate() {
            assertThat(mockMvc.put().uri("/api/v1/accounts/me/nickname")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "newnickname"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.nickname").isEqualTo("newnickname");
        }

        @Test
        @DisplayName("returns 409 for duplicate nickname")
        void duplicateNickname() {
            accountRepository.save(Account.builder()
                    .email("other@example.com")
                    .nickname("existing")
                    .provider("google")
                    .providerId("google-456")
                    .joinedAt(Instant.now())
                    .build());

            assertThat(mockMvc.put().uri("/api/v1/accounts/me/nickname")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "existing"}
                                    """))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("VALID_003");
        }

        @Test
        @DisplayName("returns 400 for nickname too short")
        void tooShort() {
            assertThat(mockMvc.put().uri("/api/v1/accounts/me/nickname")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "ab"}
                                    """))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(false);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.put().uri("/api/v1/accounts/me/nickname")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"nickname": "newnick"}
                                    """))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== 6. Tag management =====

    @Nested
    @DisplayName("Tag management API")
    class TagManagement {

        @Test
        @DisplayName("returns tag list for user with tags")
        void getTagsWithData() {
            Tag tag = tagRepository.save(Tag.builder().title("spring").build());
            accountTagRepository.save(AccountTag.builder().account(testAccount).tag(tag).build());

            assertThat(mockMvc.get().uri("/api/v1/accounts/me/tags")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data[0].title").isEqualTo("spring");
        }

        @Test
        @DisplayName("returns empty list for user without tags")
        void getTagsEmpty() {
            assertThat(mockMvc.get().uri("/api/v1/accounts/me/tags")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data").asArray().isEmpty();
        }

        @Test
        @DisplayName("adds a valid tag")
        void addTag() {
            assertThat(mockMvc.post().uri("/api/v1/accounts/me/tags")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tagTitle": "java"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 for empty tag title")
        void addEmptyTag() {
            assertThat(mockMvc.post().uri("/api/v1/accounts/me/tags")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tagTitle": ""}
                                    """))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("removes an existing tag")
        void removeTag() {
            Tag tag = tagRepository.save(Tag.builder().title("remove-me").build());
            accountTagRepository.save(AccountTag.builder().account(testAccount).tag(tag).build());

            assertThat(mockMvc.delete().uri("/api/v1/accounts/me/tags")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tagTitle": "remove-me"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 when removing non-existing tag")
        void removeNonExistingTag() {
            assertThat(mockMvc.delete().uri("/api/v1/accounts/me/tags")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"tagTitle": "nonexistent"}
                                    """))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    // ===== 7. Zone management =====

    @Nested
    @DisplayName("Zone management API")
    class ZoneManagement {

        private Zone testZone;

        @BeforeEach
        void setUpZone() {
            testZone = zoneRepository.findByCityAndProvince("Seoul", "none")
                    .orElseGet(() -> zoneRepository.save(Zone.builder()
                            .country("Korea")
                            .city("Seoul")
                            .localNameOfCity("none")
                            .province("none")
                            .build()));
        }

        @Test
        @DisplayName("returns zone list for user with zones")
        void getZonesWithData() {
            accountZoneRepository.save(AccountZone.builder().account(testAccount).zone(testZone).build());

            assertThat(mockMvc.get().uri("/api/v1/accounts/me/zones")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data[0].city").isEqualTo("Seoul");
        }

        @Test
        @DisplayName("returns empty list for user without zones")
        void getZonesEmpty() {
            assertThat(mockMvc.get().uri("/api/v1/accounts/me/zones")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data").asArray().isEmpty();
        }

        @Test
        @DisplayName("adds a valid zone")
        void addZone() {
            assertThat(mockMvc.post().uri("/api/v1/accounts/me/zones")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"zoneName": "Korea/Seoul"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 for non-existing zone")
        void addNonExistingZone() {
            assertThat(mockMvc.post().uri("/api/v1/accounts/me/zones")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"zoneName": "Unknown/City"}
                                    """))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("removes an existing zone")
        void removeZone() {
            accountZoneRepository.save(AccountZone.builder().account(testAccount).zone(testZone).build());

            assertThat(mockMvc.delete().uri("/api/v1/accounts/me/zones")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"zoneName": "Korea/Seoul"}
                                    """))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 when removing non-existing zone")
        void removeNonExistingZone() {
            assertThat(mockMvc.delete().uri("/api/v1/accounts/me/zones")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"zoneName": "Unknown/City"}
                                    """))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }
}
