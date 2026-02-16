package com.jokahobby.modules.event.event;

import com.jokahobby.infra.config.AppProperties;
import com.jokahobby.infra.mail.EmailMessage;
import com.jokahobby.infra.mail.EmailService;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.Enrollment;
import com.jokahobby.modules.event.Event;
import com.jokahobby.modules.hobby.Hobby;
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

@Slf4j
@Async
@Component
@RequiredArgsConstructor
public class EnrollmentEventListener {

    private final NotificationRepository notificationRepository;
    private final AppProperties appProperties;
    private final TemplateEngine templateEngine;
    private final EmailService emailService;

    @EventListener
    @Transactional
    public void handleEnrollmentEvent(EnrollmentEvent enrollmentEvent) {
        Enrollment enrollment = enrollmentEvent.getEnrollment();
        Account account = enrollment.getAccount();
        Event event = enrollment.getEvent();
        Hobby hobby = event.getHobby();
        log.info("Processing EnrollmentEvent enrollmentId={}, eventId={}", enrollment.getId(), event.getId());

        if(account.isHobbyEnrollmentResultByEmail()){
            sendEnrollmentResultEmail(account, hobby, event, enrollmentEvent);
        }

        if(account.isHobbyEnrollmentResultByWeb()){
            createNotification(account, hobby, event, enrollmentEvent);
        }

    }

    private void sendEnrollmentResultEmail(Account account, Hobby hobby, Event event, EnrollmentEvent enrollmentEvent) {
        Context context = new Context();
        context.setVariable("nickname", account.getNickname());
        context.setVariable("link", "/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId());
        context.setVariable("linkName", hobby.getTitle());
        context.setVariable("message", enrollmentEvent.getMessage());
        context.setVariable("host", appProperties.getHost());
        String message = templateEngine.process("mail/simple-link", context);

        EmailMessage emailMessage = EmailMessage.builder()
                .subject("JokaHobby, " + event.getTitle() + " event enrollment result.")
                .to(account.getEmail())
                .message(message)
                .build();

        emailService.sendEmail(emailMessage);
    }

    private void createNotification(Account account, Hobby hobby, Event event, EnrollmentEvent enrollmentEvent) {
        Notification notification = Notification.builder()
                .title(hobby.getTitle() + " / " + event.getTitle())
                .link("/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId())
                .checked(false)
                .message(enrollmentEvent.getMessage())
                .account(account)
                .notificationType(NotificationType.EVENT_ENROLLMENT)
                .build();
        notificationRepository.save(notification);
    }
}
