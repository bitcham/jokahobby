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
class HobbyApiControllerTest extends AbstractContainerBaseTest {

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
                .build());
        accessToken = jwtProvider.createAccessToken(testAccount.getId());
    }

    private String bearer() {
        return "Bearer " + accessToken;
    }

    private Hobby createPublishedHobby(String path, String title) {
        Hobby hobby = hobbyRepository.save(Hobby.builder()
                .path(path)
                .title(title)
                .shortDescription("Short desc")
                .fullDescription("Full desc")
                .published(true)
                .publishedDateTime(Instant.now())
                .recruiting(true)
                .memberCount(1)
                .build());
        hobbyManagerRepository.save(HobbyManager.builder().hobby(hobby).account(testAccount).build());
        return hobby;
    }

    // ===== GET /api/v1/hobbies =====

    @Nested
    @DisplayName("GET /api/v1/hobbies")
    class GetHobbyList {

        @Test
        @DisplayName("returns paginated published hobbies")
        void returnsPaginatedList() {
            createPublishedHobby("hobby1", "Hobby One");
            createPublishedHobby("hobby2", "Hobby Two");

            assertThat(mockMvc.get().uri("/api/v1/hobbies?page=0&size=16"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);

            assertThat(mockMvc.get().uri("/api/v1/hobbies?page=0&size=16"))
                    .bodyJson()
                    .extractingPath("$.data.content").asArray().hasSize(2);
        }

        @Test
        @DisplayName("excludes closed hobbies")
        void excludesClosed() {
            createPublishedHobby("open-hobby", "Open Hobby");
            hobbyRepository.save(Hobby.builder()
                    .path("closed-hobby")
                    .title("Closed Hobby")
                    .shortDescription("Short")
                    .fullDescription("Full")
                    .published(true)
                    .closed(true)
                    .publishedDateTime(Instant.now())
                    .build());

            assertThat(mockMvc.get().uri("/api/v1/hobbies?page=0&size=16"))
                    .bodyJson()
                    .extractingPath("$.data.content").asArray().hasSize(1);
        }

        @Test
        @DisplayName("filters by country")
        void filtersByCountry() {
            Hobby hobby = createPublishedHobby("korea-hobby", "Korea Hobby");
            Zone zone = zoneRepository.findByCityAndProvince("Seoul", "none")
                    .orElseGet(() -> zoneRepository.save(Zone.builder()
                            .country("Korea").city("Seoul").localNameOfCity("서울").province("none").build()));
            hobbyZoneRepository.save(HobbyZone.builder().hobby(hobby).zone(zone).build());

            createPublishedHobby("no-zone-hobby", "No Zone Hobby");

            assertThat(mockMvc.get().uri("/api/v1/hobbies?country=Korea"))
                    .bodyJson()
                    .extractingPath("$.data.content").asArray().hasSize(1);
        }

        @Test
        @DisplayName("sorts by POPULAR")
        void sortsByPopular() {
            Hobby h1 = hobbyRepository.saveAndFlush(Hobby.builder()
                    .path("less-popular")
                    .title("Less Popular")
                    .shortDescription("Short desc")
                    .fullDescription("Full desc")
                    .published(true)
                    .publishedDateTime(Instant.now())
                    .recruiting(true)
                    .memberCount(5)
                    .build());
            hobbyManagerRepository.save(HobbyManager.builder().hobby(h1).account(testAccount).build());

            Hobby h2 = hobbyRepository.saveAndFlush(Hobby.builder()
                    .path("more-popular")
                    .title("More Popular")
                    .shortDescription("Short desc")
                    .fullDescription("Full desc")
                    .published(true)
                    .publishedDateTime(Instant.now())
                    .recruiting(true)
                    .memberCount(10)
                    .build());
            hobbyManagerRepository.save(HobbyManager.builder().hobby(h2).account(testAccount).build());

            assertThat(mockMvc.get().uri("/api/v1/hobbies?sortType=POPULAR"))
                    .bodyJson()
                    .extractingPath("$.data.content[0].title").isEqualTo("More Popular");
        }

        @Test
        @DisplayName("accessible without authentication")
        void noAuthRequired() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies"))
                    .hasStatusOk();
        }

        @Test
        @DisplayName("returns empty page when no hobbies")
        void emptyPage() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.content").asArray().isEmpty();
        }
    }

    // ===== GET /api/v1/hobbies/search =====

    @Nested
    @DisplayName("GET /api/v1/hobbies/search")
    class SearchHobbies {

        @Test
        @DisplayName("searches by keyword in title")
        void searchByTitle() {
            createPublishedHobby("spring-hobby", "Spring Boot Study");
            createPublishedHobby("react-hobby", "React Study");

            assertThat(mockMvc.get().uri("/api/v1/hobbies/search?keyword=Spring"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.content").asArray().hasSize(1);
        }

        @Test
        @DisplayName("returns empty for non-matching keyword")
        void noMatch() {
            createPublishedHobby("spring-hobby", "Spring Study");

            assertThat(mockMvc.get().uri("/api/v1/hobbies/search?keyword=nonexistent"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.content").asArray().isEmpty();
        }

        @Test
        @DisplayName("accessible without authentication")
        void noAuthRequired() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/search?keyword=test"))
                    .hasStatusOk();
        }
    }

    // ===== POST /api/v1/hobbies =====

    @Nested
    @DisplayName("POST /api/v1/hobbies")
    class CreateHobby {

        @Test
        @DisplayName("creates hobby with valid data")
        void validCreate() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "path": "new-hobby",
                                        "title": "New Hobby",
                                        "shortDescription": "Short description",
                                        "fullDescription": "Full description of the hobby"
                                    }
                                    """))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.data.title").isEqualTo("New Hobby");
        }

        @Test
        @DisplayName("returns 400 for invalid path format")
        void invalidPath() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "path": "a",
                                        "title": "New Hobby",
                                        "shortDescription": "Short",
                                        "fullDescription": "Full"
                                    }
                                    """))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 400 for missing required fields")
        void missingFields() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies")
                            .header("Authorization", bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"path": "valid-path"}
                                    """))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "path": "new-hobby",
                                        "title": "New Hobby",
                                        "shortDescription": "Short",
                                        "fullDescription": "Full"
                                    }
                                    """))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== GET /api/v1/hobbies/{path} =====

    @Nested
    @DisplayName("GET /api/v1/hobbies/{path}")
    class GetHobbyDetail {

        @Test
        @DisplayName("returns hobby detail for published hobby")
        void existingHobby() {
            createPublishedHobby("test-hobby", "Test Hobby");

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}", "test-hobby"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.title").isEqualTo("Test Hobby");
        }

        @Test
        @DisplayName("returns 404 for non-existing path")
        void nonExistingPath() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}", "nonexistent"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("RESOURCE_002");
        }

        @Test
        @DisplayName("includes membership info for authenticated user")
        void authenticatedUser() {
            createPublishedHobby("test-hobby", "Test Hobby");

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}", "test-hobby")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.isManager").isEqualTo(true);
        }

        @Test
        @DisplayName("accessible without authentication")
        void noAuthRequired() {
            createPublishedHobby("test-hobby", "Test Hobby");

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}", "test-hobby"))
                    .hasStatusOk();
        }
    }

    // ===== GET /api/v1/hobbies/{path}/members =====

    @Nested
    @DisplayName("GET /api/v1/hobbies/{path}/members")
    class GetHobbyMembers {

        @Test
        @DisplayName("returns managers and members lists")
        void returnsMembersList() {
            createPublishedHobby("test-hobby", "Test Hobby");

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/members", "test-hobby"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.managers").asArray().hasSize(1);
        }

        @Test
        @DisplayName("returns 404 for non-existing hobby")
        void nonExistingHobby() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/members", "nonexistent"))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("accessible without authentication")
        void noAuthRequired() {
            createPublishedHobby("test-hobby", "Test Hobby");

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/members", "test-hobby"))
                    .hasStatusOk();
        }
    }

    // ===== POST /api/v1/hobbies/{path}/members =====

    @Nested
    @DisplayName("POST /api/v1/hobbies/{path}/members")
    class JoinHobby {

        @Test
        @DisplayName("joins hobby successfully")
        void joinSuccess() {
            Account otherAccount = accountRepository.save(Account.builder()
                    .email("other@example.com")
                    .nickname("otheruser")
                    .provider("google")
                    .providerId("google-other")
                    .joinedAt(Instant.now())
                    .build());
            Hobby hobby = createPublishedHobby("joinable-hobby", "Joinable Hobby");

            String otherToken = "Bearer " + jwtProvider.createAccessToken(otherAccount.getId());

            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/members", "joinable-hobby")
                            .header("Authorization", otherToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 when not joinable")
        void notJoinable() {
            Hobby hobby = hobbyRepository.save(Hobby.builder()
                    .path("not-joinable")
                    .title("Not Joinable")
                    .shortDescription("Short")
                    .fullDescription("Full")
                    .published(false)
                    .build());
            hobbyManagerRepository.save(HobbyManager.builder().hobby(hobby).account(testAccount).build());

            Account otherAccount = accountRepository.save(Account.builder()
                    .email("other2@example.com")
                    .nickname("other2")
                    .provider("google")
                    .providerId("google-other2")
                    .joinedAt(Instant.now())
                    .build());
            String otherToken = "Bearer " + jwtProvider.createAccessToken(otherAccount.getId());

            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/members", "not-joinable")
                            .header("Authorization", otherToken))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("HOBBY_005");
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            createPublishedHobby("joinable-hobby", "Joinable Hobby");

            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/members", "joinable-hobby"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== DELETE /api/v1/hobbies/{path}/members =====

    @Nested
    @DisplayName("DELETE /api/v1/hobbies/{path}/members")
    class LeaveHobby {

        @Test
        @DisplayName("leaves hobby successfully")
        void leaveSuccess() {
            Account otherAccount = accountRepository.save(Account.builder()
                    .email("other@example.com")
                    .nickname("otheruser")
                    .provider("google")
                    .providerId("google-other")
                    .joinedAt(Instant.now())
                    .build());
            Hobby hobby = createPublishedHobby("leave-hobby", "Leave Hobby");
            hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(otherAccount).build());

            String otherToken = "Bearer " + jwtProvider.createAccessToken(otherAccount.getId());

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/members", "leave-hobby")
                            .header("Authorization", otherToken))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 when not a member")
        void notMember() {
            createPublishedHobby("some-hobby", "Some Hobby");
            Account otherAccount = accountRepository.save(Account.builder()
                    .email("other3@example.com")
                    .nickname("other3")
                    .provider("google")
                    .providerId("google-other3")
                    .joinedAt(Instant.now())
                    .build());
            String otherToken = "Bearer " + jwtProvider.createAccessToken(otherAccount.getId());

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/members", "some-hobby")
                            .header("Authorization", otherToken))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("HOBBY_006");
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            createPublishedHobby("leave-hobby", "Leave Hobby");

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/members", "leave-hobby"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== DELETE /api/v1/hobbies/{path} =====

    @Nested
    @DisplayName("DELETE /api/v1/hobbies/{path}")
    class DeleteHobby {

        @Test
        @DisplayName("deletes removable hobby by manager")
        void deleteSuccess() {
            Hobby hobby = hobbyRepository.save(Hobby.builder()
                    .path("removable-hobby")
                    .title("Removable Hobby")
                    .shortDescription("Short")
                    .fullDescription("Full")
                    .published(false)
                    .build());
            hobbyManagerRepository.save(HobbyManager.builder().hobby(hobby).account(testAccount).build());

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}", "removable-hobby")
                            .header("Authorization", bearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            Account otherAccount = accountRepository.save(Account.builder()
                    .email("other@example.com")
                    .nickname("otheruser")
                    .provider("google")
                    .providerId("google-other")
                    .joinedAt(Instant.now())
                    .build());
            Hobby hobby = hobbyRepository.save(Hobby.builder()
                    .path("other-hobby")
                    .title("Other Hobby")
                    .shortDescription("Short")
                    .fullDescription("Full")
                    .published(false)
                    .build());
            hobbyManagerRepository.save(HobbyManager.builder().hobby(hobby).account(otherAccount).build());

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}", "other-hobby")
                            .header("Authorization", bearer()))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}", "some-hobby"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }
}
