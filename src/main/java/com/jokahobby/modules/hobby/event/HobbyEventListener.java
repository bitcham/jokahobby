package com.jokahobby.modules.hobby.event;

import com.jokahobby.infra.config.AppProperties;
import com.jokahobby.infra.mail.EmailMessage;
import com.jokahobby.infra.mail.EmailService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountPredicates;
import com.jokahobby.modules.account.AccountRepository;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyRepository;
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


@Slf4j
@Async
@Component
@Transactional
@RequiredArgsConstructor
public class HobbyEventListener {

    private final HobbyRepository hobbyRepository;
    private final AccountRepository accountRepository;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final AppProperties appProperties;
    private final NotificationRepository notificationRepository;

    @EventListener
    public void handleHobbyCreatedEvent(HobbyCreatedEvent event) {
        Hobby hobby = hobbyRepository.findHobbyWithTagsAndZonesById(event.getHobby().getId());
        Iterable<Account> accounts = accountRepository.findAll(AccountPredicates.findByTagsAndZones(hobby.getTags(), hobby.getZones()));
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
    public void handleHobbyUpdateEvent(HobbyUpdateEvent hobbyUpdateEvent) {
        Hobby hobby = hobbyRepository.findHobbyWithManagersAndMembersById(hobbyUpdateEvent.getHobby().getId());
        Set<Account> accounts = new HashSet<>();
        accounts.addAll(hobby.getManagers());
        accounts.addAll(hobby.getMembers());
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
