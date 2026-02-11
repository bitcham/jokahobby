package com.jokahobby.modules.event;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.event.EnrollmentAcceptedEvent;
import com.jokahobby.modules.event.event.EnrollmentRejectedEvent;
import com.jokahobby.modules.event.form.EventForm;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Event createEvent(Event event) {
        eventPublisher.publishEvent(new HobbyUpdateEvent(event.getHobby(),
                "'" + event.getTitle() + "' event created"));
        return eventRepository.save(event);
    }

    @Transactional
    public void updateEvent(Event event, @Valid EventForm eventForm) {
        event.updateDetails(
                eventForm.getTitle(),
                eventForm.getDescription(),
                eventForm.getEndEnrollmentDateTime().toInstant(ZoneOffset.UTC),
                eventForm.getStartDateTime().toInstant(ZoneOffset.UTC),
                eventForm.getEndDateTime().toInstant(ZoneOffset.UTC),
                eventForm.getLimitOfEnrollments()
        );
        event.acceptWaitingList();
        eventPublisher.publishEvent(new HobbyUpdateEvent(event.getHobby(),
                "'" + event.getTitle() + "' event updated. Please check the details."));
    }

    @Transactional
    public void deleteEvent(Event event) {
        event.getEnrollments().forEach(Enrollment::softDelete);
        event.softDelete();
        eventPublisher.publishEvent(new HobbyUpdateEvent(event.getHobby(),
                "'" + event.getTitle() + "' event canceled."));
    }

    @Transactional
    public void newEnrollment(Event event, Account account) {
        if (!enrollmentRepository.existsByEventAndAccount(event, account)) {
            Enrollment enrollment = Enrollment.builder()
                    .enrolledAt(Instant.now())
                    .accepted(event.isAbleToAcceptWaitingEnrollment())
                    .account(account)
                    .build();
            event.addEnrollment(enrollment);
            enrollmentRepository.save(enrollment);
        }
    }

    @Transactional
    public void cancelEnrollment(Event event, Account account) {
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        if(!enrollment.isAttended()){
            event.removeEnrollment(enrollment);
            enrollmentRepository.delete(enrollment);
            event.acceptNextWaitingEnrollment();
        }
    }

    @Transactional
    public void acceptEnrollment(Event event, Enrollment enrollment) {
        event.accept(enrollment);
        eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment));
    }

    @Transactional
    public void rejectEnrollment(Event event, Enrollment enrollment) {
        event.reject(enrollment);
        eventPublisher.publishEvent(new EnrollmentRejectedEvent(enrollment));
    }

    @Transactional
    public void checkInEnrollment(Enrollment enrollment) {
        enrollment.checkIn();
    }

    @Transactional
    public void cancelCheckInEnrollment(Enrollment enrollment) {
        enrollment.cancelCheckIn();
    }
}
