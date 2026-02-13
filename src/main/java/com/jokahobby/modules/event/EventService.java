package com.jokahobby.modules.event;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.event.EnrollmentAcceptedEvent;
import com.jokahobby.modules.event.event.EnrollmentRejectedEvent;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Event createEvent(Event event) {
        eventPublisher.publishEvent(new HobbyUpdateEvent(event.getHobby(),
                "'" + event.getTitle() + "' event created"));
        return eventRepository.save(event);
    }

    public void updateEvent(Event event, String title, String description,
                            Instant endEnrollmentDateTime, Instant startDateTime,
                            Instant endDateTime, Integer limitOfEnrollments) {
        event.updateDetails(title, description, endEnrollmentDateTime,
                startDateTime, endDateTime, limitOfEnrollments);
        event.acceptWaitingList();
        eventPublisher.publishEvent(new HobbyUpdateEvent(event.getHobby(),
                "'" + event.getTitle() + "' event updated. Please check the details."));
    }

    public void deleteEvent(Event event) {
        event.getEnrollments().forEach(Enrollment::softDelete);
        event.softDelete();
        eventPublisher.publishEvent(new HobbyUpdateEvent(event.getHobby(),
                "'" + event.getTitle() + "' event canceled."));
    }

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

    public void cancelEnrollment(Event event, Account account) {
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        if(!enrollment.isAttended()){
            event.removeEnrollment(enrollment);
            enrollmentRepository.delete(enrollment);
            event.acceptNextWaitingEnrollment();
        }
    }

    public void acceptEnrollment(Event event, Enrollment enrollment) {
        event.accept(enrollment);
        eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment));
    }

    public void rejectEnrollment(Event event, Enrollment enrollment) {
        event.reject(enrollment);
        eventPublisher.publishEvent(new EnrollmentRejectedEvent(enrollment));
    }

    public void checkInEnrollment(Enrollment enrollment) {
        enrollment.checkIn();
    }

    public void cancelCheckInEnrollment(Enrollment enrollment) {
        enrollment.cancelCheckIn();
    }
}
