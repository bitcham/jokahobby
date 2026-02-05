package com.jokahobby.modules.hobby.event;

import com.jokahobby.infra.config.AppProperties;
import com.jokahobby.infra.mail.EmailMessage;
import com.jokahobby.infra.mail.EmailService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountPredicates;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.hobby.*;
import com.jokahobby.modules.notification.Notification;
import com.jokahobby.modules.notification.NotificationRepository;
import com.jokahobby.modules.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@Async
@Component
@RequiredArgsConstructor
public class HobbyEventListener {

    private final HobbyRepository hobbyRepository;
    private final HobbyTagRepository hobbyTagRepository;
    private final HobbyZoneRepository hobbyZoneRepository;
    private final HobbyManagerRepository hobbyManagerRepository;
    private final HobbyMemberRepository hobbyMemberRepository;
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;
    private final NotificationRepository notificationRepository;

    @EventListener
    @Transactional
    public void handleHobbyCreatedEvent(HobbyCreatedEvent event) {
        Hobby hobby = hobbyRepository.findById(event.getHobby().getId()).orElseThrow();
        Set<com.jokahobby.modules.tag.Tag> tags = hobbyTagRepository.findAllByHobbyId(hobby.getId()).stream()
                .map(HobbyTag::getTag).collect(Collectors.toSet());
        Set<com.jokahobby.modules.zone.Zone> zones = hobbyZoneRepository.findAllByHobbyId(hobby.getId()).stream()
                .map(HobbyZone::getZone).collect(Collectors.toSet());
        Iterable<Account> accounts = accountRepository.findAll(AccountPredicates.findByTagsAndZones(tags, zones));
        accounts.forEach(account -> {
            if (account.isHobbyCreatedByEmail()) {
                sendHobbyCreatedEmail(account, hobby,
                        "A new hobby('" + hobby.getTitle() + "') is created.", "JokaHobby, '" + hobby.getTitle() + "' has been created.");
            }
            if (account.isHobbyCreatedByWeb()) {
                createNotification(account, hobby, hobby.getShortDescription(), NotificationType.HOBBY_CREATED);
            }
        });
    }

    @EventListener
    @Transactional
    public void handleHobbyUpdateEvent(HobbyUpdateEvent hobbyUpdateEvent) {
        Hobby hobby = hobbyRepository.findById(hobbyUpdateEvent.getHobby().getId()).orElseThrow();
        Set<Account> accounts = new HashSet<>();
        accounts.addAll(hobbyManagerRepository.findAllByHobbyId(hobby.getId()).stream()
                .map(HobbyManager::getAccount).toList());
        accounts.addAll(hobbyMemberRepository.findAllByHobbyId(hobby.getId()).stream()
                .map(HobbyMember::getAccount).toList());
        accounts.forEach(account -> {
            if (account.isHobbyUpdatedByEmail()) {
                sendHobbyCreatedEmail(account, hobby, hobbyUpdateEvent.getMessage(), "JokaHobby, '" + hobby.getTitle() + "' has new event.");
            }
            if (account.isHobbyUpdatedByWeb()) {
                createNotification(account, hobby, hobbyUpdateEvent.getMessage(), NotificationType.HOBBY_UPDATED);
            }
        });
    }

    private void createNotification(Account account, Hobby hobby, String message, NotificationType notificationType) {
        Notification notification = new Notification();
        notification.setTitle( hobby.getTitle());
        notification.setLink("/hobby/" + hobby.getEncodedPath());
        notification.setChecked(false);
        notification.setCreatedDateTime(LocalDateTime.now());
        notification.setMessage(message);
        notification.setAccount(account);
        notification.setNotificationType(notificationType);
        notificationRepository.save(notification);
    }

    private void sendHobbyCreatedEmail(Account account, Hobby hobby, String contextMessage, String emailSubject) {
        Context context = new Context();
        context.setVariable("nickname", account.getNickname());
        context.setVariable("link", "/hobby/" + hobby.getEncodedPath());
        context.setVariable("linkName", hobby.getTitle());
        context.setVariable("message", contextMessage);
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        EmailMessage emailMessage = EmailMessage.builder()
                .subject(emailSubject)
                .to(account.getEmail())
                .message(message)
                .build();
        emailService.sendEmail(emailMessage);
    }
}
