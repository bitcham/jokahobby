package com.jokahobby.modules.event;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.Hobby;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;

    public Event createEvent(Event event) {
        return eventRepository.save(event);
    }

    public List<Enrollment> updateEvent(Event event, String title, String description,
                                        Instant endEnrollmentDateTime, Instant startDateTime,
                                        Instant endDateTime, Integer limitOfEnrollments) {
        event.updateDetails(title, description, endEnrollmentDateTime,
                startDateTime, endDateTime, limitOfEnrollments);
        return event.acceptWaitingList();
    }

    public void deleteEvent(Event event) {
        event.getEnrollments().forEach(Enrollment::softDelete);
        event.softDelete();
    }

    public Enrollment newEnrollment(Event event, Account account) {
        if (enrollmentRepository.existsByEventAndAccount(event, account)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_ENROLLABLE);
        }

        Enrollment enrollment = Enrollment.builder()
                .enrolledAt(Instant.now())
                .accepted(event.isAbleToAcceptWaitingEnrollment())
                .account(account)
                .build();
        event.addEnrollment(enrollment);
        enrollmentRepository.save(enrollment);
        return enrollment;
    }

    public Enrollment cancelEnrollment(Event event, Account account) {
        Enrollment enrollment = enrollmentRepository.findByEventAndAccount(event, account);
        if (!enrollment.isAttended()) {
            event.removeEnrollment(enrollment);
            enrollmentRepository.delete(enrollment);
            return event.acceptNextWaitingEnrollment();
        }
        return null;
    }

    public void acceptEnrollment(Event event, Enrollment enrollment) {
        event.accept(enrollment);
    }

    public void rejectEnrollment(Event event, Enrollment enrollment) {
        event.reject(enrollment);
    }

    public void checkInEnrollment(Enrollment enrollment) {
        enrollment.checkIn();
    }

    public void cancelCheckInEnrollment(Enrollment enrollment) {
        enrollment.cancelCheckIn();
    }

    public List<Event> getEventsByHobby(Hobby hobby) {
        return eventRepository.findByHobbyOrderByStartDateTime(hobby);
    }

    public Event getEventWithHobbyCheck(Long eventId, Hobby hobby) {
        Event event = eventRepository.findWithEnrollmentsById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        if (!event.getHobby().equals(hobby)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        return event;
    }

    public Event getEventWithHobbyCheckForUpdate(Long eventId, Hobby hobby) {
        eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        Event event = eventRepository.findWithEnrollmentsById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        if (!event.getHobby().equals(hobby)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        return event;
    }

    public Enrollment getEnrollment(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }
}
