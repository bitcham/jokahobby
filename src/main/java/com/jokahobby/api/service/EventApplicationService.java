package com.jokahobby.api.service;

import com.jokahobby.api.dto.request.EventCreateRequest;
import com.jokahobby.api.dto.request.EventUpdateRequest;
import com.jokahobby.api.dto.response.EventListResponse;
import com.jokahobby.api.dto.response.EventResponse;
import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.*;
import com.jokahobby.modules.event.event.EnrollmentAcceptedEvent;
import com.jokahobby.modules.event.event.EnrollmentRejectedEvent;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyService;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class EventApplicationService {

    private final HobbyService hobbyService;
    private final EventService eventService;
    private final EventRepository eventRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EventResponse createEvent(String path, Account account, EventCreateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Event event = request.toEntity(hobby, account);
        Event saved = eventService.createEvent(event);
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby,
                "'" + saved.getTitle() + "' event created"));
        return EventResponse.from(saved, account);
    }

    @Transactional(readOnly = true)
    public List<EventListResponse> getEvents(String path) {
        Hobby hobby = hobbyService.getHobby(path);
        List<Event> events = eventRepository.findByHobbyOrderByStartDateTime(hobby);
        return events.stream().map(EventListResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(String path, Long eventId, Account account) {
        Hobby hobby = hobbyService.getHobby(path);
        Event event = getEventOfHobby(eventId, hobby);
        return EventResponse.from(event, account);
    }

    public EventResponse updateEvent(String path, Long eventId, Account account, EventUpdateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Event event = getEventOfHobby(eventId, hobby);

        if (request.limitOfEnrollments() < event.getNumberOfAcceptedEnrollments()) {
            throw new BusinessException(ErrorCode.EVENT_ENROLLMENT_LIMIT_TOO_LOW);
        }

        List<Enrollment> promotedEnrollments = eventService.updateEvent(event, request.title(), request.description(),
                request.endEnrollmentDateTime(), request.startDateTime(),
                request.endDateTime(), request.limitOfEnrollments());

        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby,
                "'" + event.getTitle() + "' event updated. Please check the details."));
        promotedEnrollments.forEach(enrollment ->
                eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment)));

        return EventResponse.from(event, account);
    }

    public void deleteEvent(String path, Long eventId, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Event event = getEventOfHobby(eventId, hobby);
        eventService.deleteEvent(event);
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby,
                "'" + event.getTitle() + "' event canceled."));
    }

    public void enroll(String path, Long eventId, Account account) {
        Hobby hobby = hobbyService.getHobby(path);
        Event event = getEventOfHobby(eventId, hobby);

        if (!event.isEnrollableFor(account)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_ENROLLABLE);
        }

        Enrollment enrollment = eventService.newEnrollment(event, account);
        if (enrollment != null && enrollment.isAccepted()) {
            eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment));
        }
    }

    public void disenroll(String path, Long eventId, Account account) {
        Hobby hobby = hobbyService.getHobby(path);
        Event event = getEventOfHobby(eventId, hobby);

        if (!event.isDisenrollableFor(account)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_DISENROLLABLE);
        }

        Enrollment promotedEnrollment = eventService.cancelEnrollment(event, account);
        if (promotedEnrollment != null) {
            eventPublisher.publishEvent(new EnrollmentAcceptedEvent(promotedEnrollment));
        }
    }

    public void acceptEnrollment(String path, Long enrollmentId, Account account) {
        hobbyService.getHobbyWithManagerCheck(account, path);
        Enrollment enrollment = getEnrollment(enrollmentId);
        Event event = enrollment.getEvent();

        if (!event.canAccept(enrollment)) {
            throw new BusinessException(ErrorCode.EVENT_CANNOT_ACCEPT);
        }

        eventService.acceptEnrollment(event, enrollment);
        eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment));
    }

    public void rejectEnrollment(String path, Long enrollmentId, Account account) {
        hobbyService.getHobbyWithManagerCheck(account, path);
        Enrollment enrollment = getEnrollment(enrollmentId);
        Event event = enrollment.getEvent();

        if (!event.canReject(enrollment)) {
            throw new BusinessException(ErrorCode.EVENT_CANNOT_REJECT);
        }

        eventService.rejectEnrollment(event, enrollment);
        eventPublisher.publishEvent(new EnrollmentRejectedEvent(enrollment));
    }

    public void checkIn(String path, Long enrollmentId, Account account) {
        hobbyService.getHobbyWithManagerCheck(account, path);
        Enrollment enrollment = getEnrollment(enrollmentId);
        eventService.checkInEnrollment(enrollment);
    }

    public void cancelCheckIn(String path, Long enrollmentId, Account account) {
        hobbyService.getHobbyWithManagerCheck(account, path);
        Enrollment enrollment = getEnrollment(enrollmentId);
        eventService.cancelCheckInEnrollment(enrollment);
    }

    private Event getEventOfHobby(Long eventId, Hobby hobby) {
        Event event = eventRepository.findWithEnrollmentsById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND));
        if (!event.getHobby().equals(hobby)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_FOUND);
        }
        return event;
    }

    private Enrollment getEnrollment(Long enrollmentId) {
        return enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));
    }
}
