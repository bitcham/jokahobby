package com.jokahobby.api.controller.v1;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.infra.security.jwt.JwtProvider;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.event.*;
import com.jokahobby.modules.hobby.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@MockMvcTest
class EventApiControllerTest extends AbstractContainerBaseTest {

    @Autowired MockMvcTester mockMvc;
    @Autowired AccountRepository accountRepository;
    @Autowired HobbyRepository hobbyRepository;
    @Autowired HobbyManagerRepository hobbyManagerRepository;
    @Autowired HobbyMemberRepository hobbyMemberRepository;
    @Autowired EventRepository eventRepository;
    @Autowired EnrollmentRepository enrollmentRepository;
    @Autowired JwtProvider jwtProvider;

    private Account managerAccount;
    private Account memberAccount;
    private String managerToken;
    private String memberToken;
    private Hobby hobby;

    @BeforeEach
    void setUp() {
        managerAccount = accountRepository.save(Account.builder()
                .email("manager@example.com")
                .nickname("manager")
                .provider("google")
                .providerId("google-manager")
                .joinedAt(Instant.now())
                .build());
        managerToken = jwtProvider.createAccessToken(managerAccount.getId());

        memberAccount = accountRepository.save(Account.builder()
                .email("member@example.com")
                .nickname("member")
                .provider("google")
                .providerId("google-member")
                .joinedAt(Instant.now())
                .build());
        memberToken = jwtProvider.createAccessToken(memberAccount.getId());

        hobby = hobbyRepository.save(Hobby.builder()
                .path("test-hobby")
                .title("Test Hobby")
                .shortDescription("Short desc")
                .fullDescription("Full desc")
                .published(true)
                .publishedDateTime(Instant.now())
                .recruiting(true)
                .memberCount(2)
                .build());
        hobbyManagerRepository.save(HobbyManager.builder().hobby(hobby).account(managerAccount).build());
        hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(memberAccount).build());
    }

    private String managerBearer() {
        return "Bearer " + managerToken;
    }

    private String memberBearer() {
        return "Bearer " + memberToken;
    }

    private String futureInstant(int hoursFromNow) {
        return "\"" + Instant.now().plus(hoursFromNow, ChronoUnit.HOURS).toString() + "\"";
    }

    private Event createFcfsEvent() {
        Event event = eventRepository.save(Event.builder()
                .title("FCFS Event")
                .description("Test event description")
                .eventType(EventType.FCFS)
                .endEnrollmentDateTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .startDateTime(Instant.now().plus(2, ChronoUnit.DAYS))
                .endDateTime(Instant.now().plus(3, ChronoUnit.DAYS))
                .limitOfEnrollments(5)
                .hobby(hobby)
                .createdBy(managerAccount)
                .build());
        return event;
    }

    private Event createConfirmativeEvent() {
        Event event = eventRepository.save(Event.builder()
                .title("Confirmative Event")
                .description("Confirmative event description")
                .eventType(EventType.CONFIRMATIVE)
                .endEnrollmentDateTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .startDateTime(Instant.now().plus(2, ChronoUnit.DAYS))
                .endDateTime(Instant.now().plus(3, ChronoUnit.DAYS))
                .limitOfEnrollments(5)
                .hobby(hobby)
                .createdBy(managerAccount)
                .build());
        return event;
    }

    private Enrollment createEnrollment(Event event, Account account, boolean accepted) {
        Enrollment enrollment = enrollmentRepository.save(Enrollment.builder()
                .enrolledAt(Instant.now())
                .accepted(accepted)
                .account(account)
                .build());
        event.addEnrollment(enrollment);
        return enrollment;
    }

    // ===== POST /api/v1/hobbies/{path}/events =====

    @Nested
    @DisplayName("POST /api/v1/hobbies/{path}/events")
    class CreateEvent {

        @Test
        @DisplayName("creates FCFS event with valid data")
        void createFcfsEvent() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/events", "test-hobby")
                            .header("Authorization", managerBearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "New Event",
                                        "description": "Event description",
                                        "eventType": "FCFS",
                                        "endEnrollmentDateTime": %s,
                                        "startDateTime": %s,
                                        "endDateTime": %s,
                                        "limitOfEnrollments": 10
                                    }
                                    """.formatted(futureInstant(24), futureInstant(48), futureInstant(72))))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.data.title").isEqualTo("New Event");
        }

        @Test
        @DisplayName("creates CONFIRMATIVE event with valid data")
        void createConfirmativeEvent() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/events", "test-hobby")
                            .header("Authorization", managerBearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Confirm Event",
                                        "description": "Confirmative event",
                                        "eventType": "CONFIRMATIVE",
                                        "endEnrollmentDateTime": %s,
                                        "startDateTime": %s,
                                        "endDateTime": %s,
                                        "limitOfEnrollments": 5
                                    }
                                    """.formatted(futureInstant(24), futureInstant(48), futureInstant(72))))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.data.eventType").isEqualTo("CONFIRMATIVE");
        }

        @Test
        @DisplayName("returns 400 for missing required fields")
        void missingFields() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/events", "test-hobby")
                            .header("Authorization", managerBearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title": "Only Title"}
                                    """))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 400 for invalid dates")
        void invalidDates() {
            String pastInstant = "\"" + Instant.now().minus(1, ChronoUnit.HOURS).toString() + "\"";
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/events", "test-hobby")
                            .header("Authorization", managerBearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Bad Event",
                                        "eventType": "FCFS",
                                        "endEnrollmentDateTime": %s,
                                        "startDateTime": %s,
                                        "endDateTime": %s,
                                        "limitOfEnrollments": 5
                                    }
                                    """.formatted(pastInstant, futureInstant(48), futureInstant(72))))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/events", "test-hobby")
                            .header("Authorization", memberBearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "New Event",
                                        "eventType": "FCFS",
                                        "endEnrollmentDateTime": %s,
                                        "startDateTime": %s,
                                        "endDateTime": %s,
                                        "limitOfEnrollments": 5
                                    }
                                    """.formatted(futureInstant(24), futureInstant(48), futureInstant(72))))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/events", "test-hobby")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "New Event",
                                        "eventType": "FCFS",
                                        "endEnrollmentDateTime": %s,
                                        "startDateTime": %s,
                                        "endDateTime": %s,
                                        "limitOfEnrollments": 5
                                    }
                                    """.formatted(futureInstant(24), futureInstant(48), futureInstant(72))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== GET /api/v1/hobbies/{path}/events =====

    @Nested
    @DisplayName("GET /api/v1/hobbies/{path}/events")
    class GetEventList {

        @Test
        @DisplayName("returns all events for hobby")
        void returnsAllEvents() {
            createFcfsEvent();
            createConfirmativeEvent();

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/events", "test-hobby"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data").asArray().hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no events")
        void emptyList() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/events", "test-hobby"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data").asArray().isEmpty();
        }

        @Test
        @DisplayName("accessible without authentication")
        void noAuthRequired() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/events", "test-hobby"))
                    .hasStatusOk();
        }

        @Test
        @DisplayName("returns 404 for non-existing hobby")
        void nonExistingHobby() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/events", "nonexistent"))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    // ===== GET /api/v1/hobbies/{path}/events/{eventId} =====

    @Nested
    @DisplayName("GET /api/v1/hobbies/{path}/events/{eventId}")
    class GetEventDetail {

        @Test
        @DisplayName("returns event with enrollments")
        void returnsEventDetail() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", event.getId()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.title").isEqualTo("FCFS Event");
        }

        @Test
        @DisplayName("includes computed fields for authenticated user")
        void computedFieldsAuthenticated() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", event.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.isEnrollable").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 404 for non-existing event")
        void nonExistingEvent() {
            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", 99999))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("accessible without authentication")
        void noAuthRequired() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.get().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", event.getId()))
                    .hasStatusOk();
        }
    }

    // ===== PUT /api/v1/hobbies/{path}/events/{eventId} =====

    @Nested
    @DisplayName("PUT /api/v1/hobbies/{path}/events/{eventId}")
    class UpdateEvent {

        @Test
        @DisplayName("updates event with valid data")
        void validUpdate() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", event.getId())
                            .header("Authorization", managerBearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Event",
                                        "description": "Updated description",
                                        "endEnrollmentDateTime": %s,
                                        "startDateTime": %s,
                                        "endDateTime": %s,
                                        "limitOfEnrollments": 10
                                    }
                                    """.formatted(futureInstant(24), futureInstant(48), futureInstant(72))))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.data.title").isEqualTo("Updated Event");
        }

        @Test
        @DisplayName("returns 400 when limit below accepted count")
        void limitBelowAccepted() {
            Event event = createFcfsEvent();
            createEnrollment(event, memberAccount, true);
            createEnrollment(event, managerAccount, true);

            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", event.getId())
                            .header("Authorization", managerBearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated Event",
                                        "endEnrollmentDateTime": %s,
                                        "startDateTime": %s,
                                        "endDateTime": %s,
                                        "limitOfEnrollments": 1
                                    }
                                    """.formatted(futureInstant(24), futureInstant(48), futureInstant(72))))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", event.getId())
                            .header("Authorization", memberBearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated",
                                        "endEnrollmentDateTime": %s,
                                        "startDateTime": %s,
                                        "endDateTime": %s,
                                        "limitOfEnrollments": 5
                                    }
                                    """.formatted(futureInstant(24), futureInstant(48), futureInstant(72))))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.put().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", event.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "title": "Updated",
                                        "endEnrollmentDateTime": %s,
                                        "startDateTime": %s,
                                        "endDateTime": %s,
                                        "limitOfEnrollments": 5
                                    }
                                    """.formatted(futureInstant(24), futureInstant(48), futureInstant(72))))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== DELETE /api/v1/hobbies/{path}/events/{eventId} =====

    @Nested
    @DisplayName("DELETE /api/v1/hobbies/{path}/events/{eventId}")
    class DeleteEvent {

        @Test
        @DisplayName("soft deletes event")
        void deleteSuccess() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", event.getId())
                            .header("Authorization", managerBearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", event.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/events/{eventId}", "test-hobby", event.getId()))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== POST .../events/{eventId}/enrollments =====

    @Nested
    @DisplayName("POST /api/v1/hobbies/{path}/events/{eventId}/enrollments")
    class Enroll {

        @Test
        @DisplayName("enrolls member in FCFS event (auto-accepted)")
        void enrollFcfs() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/events/{eventId}/enrollments", "test-hobby", event.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("enrolls member in CONFIRMATIVE event (pending)")
        void enrollConfirmative() {
            Event event = createConfirmativeEvent();

            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/events/{eventId}/enrollments", "test-hobby", event.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 when already enrolled")
        void alreadyEnrolled() {
            Event event = createFcfsEvent();
            createEnrollment(event, memberAccount, true);

            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/events/{eventId}/enrollments", "test-hobby", event.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("EVENT_001");
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.post().uri("/api/v1/hobbies/{path}/events/{eventId}/enrollments", "test-hobby", event.getId()))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== DELETE .../events/{eventId}/enrollments =====

    @Nested
    @DisplayName("DELETE /api/v1/hobbies/{path}/events/{eventId}/enrollments")
    class Disenroll {

        @Test
        @DisplayName("cancels enrollment")
        void disenrollSuccess() {
            Event event = createFcfsEvent();
            createEnrollment(event, memberAccount, true);

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/events/{eventId}/enrollments", "test-hobby", event.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 when not enrolled")
        void notEnrolled() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/events/{eventId}/enrollments", "test-hobby", event.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("EVENT_002");
        }

        @Test
        @DisplayName("returns 401 without authentication")
        void unauthenticated() {
            Event event = createFcfsEvent();

            assertThat(mockMvc.delete().uri("/api/v1/hobbies/{path}/events/{eventId}/enrollments", "test-hobby", event.getId()))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    // ===== PATCH .../enrollments/{enrollmentId}/accept =====

    @Nested
    @DisplayName("PATCH /api/v1/hobbies/{path}/enrollments/{enrollmentId}/accept")
    class AcceptEnrollment {

        @Test
        @DisplayName("accepts enrollment for CONFIRMATIVE event")
        void acceptSuccess() {
            Event event = createConfirmativeEvent();
            Enrollment enrollment = createEnrollment(event, memberAccount, false);

            assertThat(mockMvc.patch().uri("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/accept", "test-hobby", enrollment.getId())
                            .header("Authorization", managerBearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 for FCFS event")
        void fcfsEvent() {
            Event event = createFcfsEvent();
            Enrollment enrollment = createEnrollment(event, memberAccount, false);

            assertThat(mockMvc.patch().uri("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/accept", "test-hobby", enrollment.getId())
                            .header("Authorization", managerBearer()))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("EVENT_004");
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            Event event = createConfirmativeEvent();
            Enrollment enrollment = createEnrollment(event, memberAccount, false);

            assertThat(mockMvc.patch().uri("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/accept", "test-hobby", enrollment.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== PATCH .../enrollments/{enrollmentId}/reject =====

    @Nested
    @DisplayName("PATCH /api/v1/hobbies/{path}/enrollments/{enrollmentId}/reject")
    class RejectEnrollment {

        @Test
        @DisplayName("rejects enrollment for CONFIRMATIVE event")
        void rejectSuccess() {
            Event event = createConfirmativeEvent();
            Enrollment enrollment = createEnrollment(event, memberAccount, true);

            assertThat(mockMvc.patch().uri("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/reject", "test-hobby", enrollment.getId())
                            .header("Authorization", managerBearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 400 for FCFS event")
        void fcfsEvent() {
            Event event = createFcfsEvent();
            Enrollment enrollment = createEnrollment(event, memberAccount, true);

            assertThat(mockMvc.patch().uri("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/reject", "test-hobby", enrollment.getId())
                            .header("Authorization", managerBearer()))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.error.code").isEqualTo("EVENT_005");
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            Event event = createConfirmativeEvent();
            Enrollment enrollment = createEnrollment(event, memberAccount, true);

            assertThat(mockMvc.patch().uri("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/reject", "test-hobby", enrollment.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== PATCH .../enrollments/{enrollmentId}/checkin =====

    @Nested
    @DisplayName("PATCH /api/v1/hobbies/{path}/enrollments/{enrollmentId}/checkin")
    class CheckIn {

        @Test
        @DisplayName("checks in enrollment")
        void checkInSuccess() {
            Event event = createFcfsEvent();
            Enrollment enrollment = createEnrollment(event, memberAccount, true);

            assertThat(mockMvc.patch().uri("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/checkin", "test-hobby", enrollment.getId())
                            .header("Authorization", managerBearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            Event event = createFcfsEvent();
            Enrollment enrollment = createEnrollment(event, memberAccount, true);

            assertThat(mockMvc.patch().uri("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/checkin", "test-hobby", enrollment.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    // ===== PATCH .../enrollments/{enrollmentId}/cancel-checkin =====

    @Nested
    @DisplayName("PATCH /api/v1/hobbies/{path}/enrollments/{enrollmentId}/cancel-checkin")
    class CancelCheckIn {

        @Test
        @DisplayName("cancels check-in")
        void cancelCheckInSuccess() {
            Event event = createFcfsEvent();
            Enrollment enrollment = createEnrollment(event, memberAccount, true);
            enrollment.checkIn();

            assertThat(mockMvc.patch().uri("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/cancel-checkin", "test-hobby", enrollment.getId())
                            .header("Authorization", managerBearer()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.success").isEqualTo(true);
        }

        @Test
        @DisplayName("returns 403 for non-manager")
        void nonManager() {
            Event event = createFcfsEvent();
            Enrollment enrollment = createEnrollment(event, memberAccount, true);

            assertThat(mockMvc.patch().uri("/api/v1/hobbies/{path}/enrollments/{enrollmentId}/cancel-checkin", "test-hobby", enrollment.getId())
                            .header("Authorization", memberBearer()))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }
}
