package com.jokahobby.infra.config;

import com.jokahobby.modules.account.*;
import com.jokahobby.modules.event.*;
import com.jokahobby.modules.hobby.*;
import com.jokahobby.modules.notification.Notification;
import com.jokahobby.modules.notification.NotificationRepository;
import com.jokahobby.modules.notification.NotificationType;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.tag.TagRepository;
import com.jokahobby.modules.zone.Zone;
import com.jokahobby.modules.zone.ZoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Profile("dev")
@Component
@RequiredArgsConstructor
@Slf4j
public class DevDataInitializer implements ApplicationRunner {

    private final AccountRepository accountRepository;
    private final TagRepository tagRepository;
    private final ZoneService zoneService;
    private final AccountTagRepository accountTagRepository;
    private final AccountZoneRepository accountZoneRepository;
    private final HobbyRepository hobbyRepository;
    private final HobbyHostRepository hobbyHostRepository;
    private final HobbyManagerRepository hobbyManagerRepository;
    private final HobbyMemberRepository hobbyMemberRepository;
    private final HobbyTagRepository hobbyTagRepository;
    private final HobbyZoneRepository hobbyZoneRepository;
    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (accountRepository.count() > 0) {
            log.info("Dev data already exists, skipping initialization.");
            return;
        }

        // 1. Accounts
        Account alice = accountRepository.save(Account.builder()
                .email("alice@dev.local")
                .nickname("alice")
                .provider("dev")
                .providerId("dev-alice")
                .joinedAt(Instant.now())
                .bio("Test manager account")
                .build());

        Account bob = accountRepository.save(Account.builder()
                .email("bob@dev.local")
                .nickname("bob")
                .provider("dev")
                .providerId("dev-bob")
                .joinedAt(Instant.now())
                .bio("Test member account")
                .build());

        Account charlie = accountRepository.save(Account.builder()
                .email("charlie@dev.local")
                .nickname("charlie")
                .provider("dev")
                .providerId("dev-charlie")
                .joinedAt(Instant.now())
                .bio("Test observer account")
                .build());

        // 2. Tags
        Tag photography = tagRepository.save(Tag.builder().title("Photography").build());
        Tag coding = tagRepository.save(Tag.builder().title("Coding").build());
        Tag gaming = tagRepository.save(Tag.builder().title("Gaming").build());
        Tag music = tagRepository.save(Tag.builder().title("Music").build());
        Tag cooking = tagRepository.save(Tag.builder().title("Cooking").build());
        Tag fitness = tagRepository.save(Tag.builder().title("Fitness").build());

        // 3. Zones (loaded by ZoneService @PostConstruct)
        Zone seoul = zoneService.findByCityAndProvince("Seoul", "none");
        Zone busan = zoneService.findByCityAndProvince("Busan", "none");

        // 4. AccountTag / AccountZone
        accountTagRepository.saveAll(List.of(
                AccountTag.builder().account(alice).tag(photography).build(),
                AccountTag.builder().account(alice).tag(coding).build(),
                AccountTag.builder().account(bob).tag(gaming).build(),
                AccountTag.builder().account(bob).tag(music).build()
        ));

        accountZoneRepository.saveAll(List.of(
                AccountZone.builder().account(alice).zone(seoul).build(),
                AccountZone.builder().account(bob).zone(busan).build()
        ));

        // 5. Hobbies
        Hobby photographyClub = hobbyRepository.save(Hobby.builder()
                .path("photography-club")
                .title("Photography Club")
                .shortDescription("A club for photography enthusiasts")
                .fullDescription("Share your photos, learn new techniques, and go on photo walks together.")
                .published(true)
                .publishedDateTime(Instant.now().minus(Duration.ofDays(7)))
                .recruiting(true)
                .recruitingUpdatedDateTime(Instant.now().minus(Duration.ofHours(2)))
                .memberCount(2)
                .build());

        Hobby codingLab = hobbyRepository.save(Hobby.builder()
                .path("coding-lab")
                .title("Coding Lab")
                .shortDescription("A lab for coders and developers")
                .fullDescription("Collaborate on projects, share knowledge, and build cool stuff.")
                .published(true)
                .publishedDateTime(Instant.now().minus(Duration.ofDays(5)))
                .recruiting(false)
                .memberCount(1)
                .build());

        Hobby draftHobby = hobbyRepository.save(Hobby.builder()
                .path("draft-hobby")
                .title("Draft Hobby")
                .shortDescription("An unpublished draft hobby")
                .fullDescription("This hobby is still in draft state.")
                .published(false)
                .recruiting(false)
                .memberCount(1)
                .build());

        // 6. HobbyHost / HobbyManager / HobbyMember
        hobbyHostRepository.saveAll(List.of(
                HobbyHost.builder().hobby(photographyClub).account(alice).build(),
                HobbyHost.builder().hobby(codingLab).account(alice).build(),
                HobbyHost.builder().hobby(draftHobby).account(alice).build()
        ));

        hobbyMemberRepository.saveAll(List.of(
                HobbyMember.builder().hobby(photographyClub).account(bob).build()
        ));

        // 7. HobbyTag / HobbyZone
        hobbyTagRepository.saveAll(List.of(
                HobbyTag.builder().hobby(photographyClub).tag(photography).build(),
                HobbyTag.builder().hobby(photographyClub).tag(coding).build(),
                HobbyTag.builder().hobby(codingLab).tag(coding).build(),
                HobbyTag.builder().hobby(codingLab).tag(gaming).build()
        ));

        hobbyZoneRepository.saveAll(List.of(
                HobbyZone.builder().hobby(photographyClub).zone(seoul).build(),
                HobbyZone.builder().hobby(photographyClub).zone(busan).build(),
                HobbyZone.builder().hobby(codingLab).zone(seoul).build()
        ));

        // 8. Events
        Instant now = Instant.now();

        Event photoWalk = eventRepository.save(Event.builder()
                .hobby(photographyClub)
                .createdBy(alice)
                .title("Weekend Photo Walk")
                .description("Let's explore the city with our cameras!")
                .endEnrollmentDateTime(now.plus(Duration.ofDays(7)))
                .startDateTime(now.plus(Duration.ofDays(8)))
                .endDateTime(now.plus(Duration.ofDays(8)).plus(Duration.ofHours(4)))
                .limitOfEnrollments(10)
                .eventType(EventType.FCFS)
                .build());

        Event photoContest = eventRepository.save(Event.builder()
                .hobby(photographyClub)
                .createdBy(alice)
                .title("Photo Contest")
                .description("Submit your best photos and win prizes!")
                .endEnrollmentDateTime(now.plus(Duration.ofDays(14)))
                .startDateTime(now.plus(Duration.ofDays(15)))
                .endDateTime(now.plus(Duration.ofDays(15)).plus(Duration.ofHours(6)))
                .limitOfEnrollments(5)
                .eventType(EventType.CONFIRMATIVE)
                .build());

        Event codeMeetup = eventRepository.save(Event.builder()
                .hobby(codingLab)
                .createdBy(alice)
                .title("Code Meetup")
                .description("Monthly coding meetup for all skill levels.")
                .endEnrollmentDateTime(now.plus(Duration.ofDays(3)))
                .startDateTime(now.plus(Duration.ofDays(4)))
                .endDateTime(now.plus(Duration.ofDays(4)).plus(Duration.ofHours(3)))
                .limitOfEnrollments(20)
                .eventType(EventType.FCFS)
                .build());

        Event hackathon = eventRepository.save(Event.builder()
                .hobby(codingLab)
                .createdBy(alice)
                .title("Hackathon")
                .description("48-hour hackathon. Build something amazing!")
                .endEnrollmentDateTime(now.plus(Duration.ofDays(10)))
                .startDateTime(now.plus(Duration.ofDays(11)))
                .endDateTime(now.plus(Duration.ofDays(13)))
                .limitOfEnrollments(8)
                .eventType(EventType.CONFIRMATIVE)
                .build());

        // 9. Enrollments
        enrollmentRepository.saveAll(List.of(
                Enrollment.builder()
                        .event(photoWalk)
                        .account(bob)
                        .enrolledAt(now.minus(Duration.ofHours(1)))
                        .accepted(true)
                        .attended(false)
                        .build(),
                Enrollment.builder()
                        .event(photoContest)
                        .account(bob)
                        .enrolledAt(now.minus(Duration.ofMinutes(30)))
                        .accepted(false)
                        .attended(false)
                        .build(),
                Enrollment.builder()
                        .event(codeMeetup)
                        .account(bob)
                        .enrolledAt(now.minus(Duration.ofMinutes(15)))
                        .accepted(true)
                        .attended(false)
                        .build()
        ));

        // 10. Notifications
        notificationRepository.saveAll(List.of(
                Notification.builder()
                        .title("Photography Club has been created")
                        .link("/hobbies/photography-club")
                        .message("A new hobby 'Photography Club' is now available!")
                        .checked(false)
                        .account(alice)
                        .notificationType(NotificationType.HOBBY_CREATED)
                        .build(),
                Notification.builder()
                        .title("Photography Club has been updated")
                        .link("/hobbies/photography-club")
                        .message("Photography Club details have been updated.")
                        .checked(false)
                        .account(alice)
                        .notificationType(NotificationType.HOBBY_UPDATED)
                        .build(),
                Notification.builder()
                        .title("You enrolled in Weekend Photo Walk")
                        .link("/hobbies/photography-club/events/" + photoWalk.getId())
                        .message("Your enrollment in 'Weekend Photo Walk' has been confirmed.")
                        .checked(false)
                        .account(bob)
                        .notificationType(NotificationType.EVENT_ENROLLMENT)
                        .build(),
                Notification.builder()
                        .title("Photography Club has been created")
                        .link("/hobbies/photography-club")
                        .message("A new hobby 'Photography Club' is now available!")
                        .checked(true)
                        .account(bob)
                        .notificationType(NotificationType.HOBBY_CREATED)
                        .build()
        ));

        log.info("""
                === Dev test data initialized ===
                Accounts: alice(host), bob(member), charlie(observer)
                Hobbies: photography-club, coding-lab, draft-hobby
                Use POST /api/v1/dev/token/{nickname} to get JWT tokens""");
    }
}
