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
                sendHobbyCreatedEmail(account, hobby);
            }
            if (account.isHobbyCreatedByWeb()) {
                saveHobbyCreatedNotification(account, hobby);
            }
        });


    }

    private void saveHobbyCreatedNotification(Account account, Hobby hobby) {
        Notification notification = new Notification();
        notification.setTitle("A new hobby('" + hobby.getTitle() + "') is created.");
        notification.setLink("/hobby/" + hobby.getEncodedPath());
        notification.setChecked(false);
        notification.setCreatedDateTime(LocalDateTime.now());
        notification.setMessage(hobby.getShortDescription());
        notification.setAccount(account);
        notification.setNotificationType(NotificationType.HOBBY_CREATED);
        notificationRepository.save(notification);
    }

    private void sendHobbyCreatedEmail(Account account, Hobby hobby) {
        Context context = new Context();
        context.setVariable("nickname", account.getNickname());
        context.setVariable("link", "/hobby/" + hobby.getEncodedPath());
        context.setVariable("linkName", hobby.getTitle());
        context.setVariable("message", "A new hobby('" + hobby.getTitle() + "') is created.");
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);
        EmailMessage emailMessage = EmailMessage.builder()
                .subject("JokaHobby, a new hobby('" + hobby.getTitle() + "') is created.")
                .to(account.getEmail())
                .message(message)
                .build();
        emailService.sendEmail(emailMessage);
    }
}
