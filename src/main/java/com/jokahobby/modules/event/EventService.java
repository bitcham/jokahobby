package com.jokahobby.modules.event;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.event.EnrollmentAcceptedEvent;
import com.jokahobby.modules.event.event.EnrollmentRejectedEvent;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.event.form.EventForm;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final ModelMapper modelMapper;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;



    public Event createEvent(Event event, Hobby hobby, Account account) {
        event.setCreatedBy(account);
        event.setCreatedDateTime(LocalDateTime.now());
        event.setHobby(hobby);
        eventPublisher.publishEvent(new HobbyUpdateEvent(event.getHobby(),
                "'" + event.getTitle() + "' event created"));
        return eventRepository.save(event);
    }

    public void updateEvent(Event event, @Valid EventForm eventForm) {
        modelMapper.map(eventForm, event);
        event.acceptWaitingList();
        eventPublisher.publishEvent(new HobbyUpdateEvent(event.getHobby(),
                "'" + event.getTitle() + "' event updated. Please check the details."));
    }

    public void deleteEvent(Event event) {
        List<Enrollment> enrollments = enrollmentRepository.findByEvent(event);
        enrollmentRepository.deleteAll(enrollments);
        eventRepository.delete(event);
        eventPublisher.publishEvent(new HobbyUpdateEvent(event.getHobby(),
                "'" + event.getTitle() + "' event canceled."));
    }

    public void newEnrollment(Event event, Account account) {
        if (!enrollmentRepository.existsByEventAndAccount(event, account)) {
            Enrollment enrollment = new Enrollment();
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollment.setAccepted(event.isAbleToAcceptWaitingEnrollment());
            enrollment.setAccount(account);
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
        enrollment.setAttended(true);
    }

    public void cancelCheckInEnrollment(Enrollment enrollment) {
        enrollment.setAttended(false);
    }
}
