package com.jokahobby.modules.event;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountFactory;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.account.WithAccount;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockMvcTest
class EventControllerTest extends AbstractContainerBaseTest {

    @Autowired MockMvc mockMvc;
    @Autowired
    HobbyFactory hobbyFactory;
    @Autowired
    AccountFactory accountFactory;
    @Autowired
    EventService eventService;
    @Autowired
    EnrollmentRepository enrollmentRepository;
    @Autowired
    AccountRepository accountRepository;

    @Test
    @DisplayName("Registration for a first-come-first-served event - Auto acceptance")
    @WithAccount("cutedog")
    void newEnrollment_to_FCFS_event_accepted() throws Exception {
        Account cham = accountFactory.createAccount("cham");
        Hobby hobby = hobbyFactory.createHobby("test-hobby", cham);
        Event event = createEvent("test-event", EventType.FCFS, 2, hobby, cham);

        mockMvc.perform(post("/hobby/" + hobby.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hobby/" + hobby.getPath() + "/events/" + event.getId()));

        Account cutedog = accountRepository.findByNickname("cutedog");
        isAccepted(cutedog, event);
    }

    @Test
    @DisplayName("Registration for a first-come-first-served event - Waiting (already full)")
    @WithAccount("cutedog")
    void newEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account cham = accountFactory.createAccount("cham");
        Hobby hobby = hobbyFactory.createHobby("test-hobby", cham);
        Event event = createEvent("test-event", EventType.FCFS, 2, hobby, cham);

        Account may = accountFactory.createAccount("may");
        Account june = accountFactory.createAccount("june");
        eventService.newEnrollment(event, may);
        eventService.newEnrollment(event, june);

        mockMvc.perform(post("/hobby/" + hobby.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hobby/" + hobby.getPath() + "/events/" + event.getId()));

        Account cutedog = accountRepository.findByNickname("cutedog");
        isNotAccepted(cutedog, event);
    }

    @Test
    @DisplayName("When a confirmed participant cancels registration for a first-come-first-served event, the next waiting person is automatically confirmed")
    @WithAccount("cutedog")
    void accepted_account_cancelEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account cutedog = accountRepository.findByNickname("cutedog");
        Account cham = accountFactory.createAccount("cham");
        Account may = accountFactory.createAccount("may");
        Hobby hobby = hobbyFactory.createHobby("test-hobby", cham);
        Event event = createEvent("test-event", EventType.FCFS, 2, hobby, cham);

        eventService.newEnrollment(event, may);
        eventService.newEnrollment(event, cutedog);
        eventService.newEnrollment(event, cham);

        isAccepted(may, event);
        isAccepted(cutedog, event);
        isNotAccepted(cham, event);

        mockMvc.perform(post("/hobby/" + hobby.getPath() + "/events/" + event.getId() + "/disenroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hobby/" + hobby.getPath() + "/events/" + event.getId()));

        isAccepted(may, event);
        isAccepted(cham, event);
        assertNull(enrollmentRepository.findByEventAndAccount(event, cutedog));
    }

    @Test
    @DisplayName("When an unconfirmed participant cancels registration for a first-come-first-served event, existing confirmed participants are maintained and there are no new confirmations")
    @WithAccount("cutedog")
    void not_accepterd_account_cancelEnrollment_to_FCFS_event_not_accepted() throws Exception {
        Account cutedog = accountRepository.findByNickname("cutedog");
        Account cham = accountFactory.createAccount("cham");
        Account may = accountFactory.createAccount("may");
        Hobby hobby = hobbyFactory.createHobby("test-hobby", cham);
        Event event = createEvent("test-event", EventType.FCFS, 2, hobby, cham);

        eventService.newEnrollment(event, may);
        eventService.newEnrollment(event, cham);
        eventService.newEnrollment(event, cutedog);

        isAccepted(may, event);
        isAccepted(cham, event);
        isNotAccepted(cutedog, event);

        mockMvc.perform(post("/hobby/" + hobby.getPath() + "/events/" + event.getId() + "/disenroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hobby/" + hobby.getPath() + "/events/" + event.getId()));

        isAccepted(may, event);
        isAccepted(cham, event);
        assertNull(enrollmentRepository.findByEventAndAccount(event, cutedog));
    }

    private void isNotAccepted(Account cham, Event event) {
        assertFalse(enrollmentRepository.findByEventAndAccount(event, cham).isAccepted());
    }

    private void isAccepted(Account account, Event event) {
        assertTrue(enrollmentRepository.findByEventAndAccount(event, account).isAccepted());
    }

    @Test
    @DisplayName("Registration for an event requiring administrator confirmation - Waiting")
    @WithAccount("cutedog")
    void newEnrollment_to_CONFIMATIVE_event_not_accepted() throws Exception {
        Account cham = accountFactory.createAccount("cham");
        Hobby hobby = hobbyFactory.createHobby("test-hobby", cham);
        Event event = createEvent("test-event", EventType.CONFIRMATIVE, 2, hobby, cham);

        mockMvc.perform(post("/hobby/" + hobby.getPath() + "/events/" + event.getId() + "/enroll")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/hobby/" + hobby.getPath() + "/events/" + event.getId()));

        Account cutedog = accountRepository.findByNickname("cutedog");
        isNotAccepted(cutedog, event);
    }

    private Event createEvent(String eventTitle, EventType eventType, int limit, Hobby hobby, Account account) {
        Event event = new Event();
        event.setEventType(eventType);
        event.setLimitOfEnrollments(limit);
        event.setTitle(eventTitle);
        event.setCreatedDateTime(LocalDateTime.now());
        event.setEndEnrollmentDateTime(LocalDateTime.now().plusDays(1));
        event.setStartDateTime(LocalDateTime.now().plusDays(1).plusHours(5));
        event.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(7));
        return eventService.createEvent(event, hobby, account);
    }

}
