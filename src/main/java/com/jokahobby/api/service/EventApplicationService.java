package com.jokahobby.api.service;

import com.jokahobby.api.dto.request.EventCreateRequest;
import com.jokahobby.api.dto.request.EventUpdateRequest;
import com.jokahobby.api.dto.response.EventListResponse;
import com.jokahobby.api.dto.response.EventResponse;
import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.event.Enrollment;
import com.jokahobby.modules.event.Event;
import com.jokahobby.modules.event.EventService;
import com.jokahobby.modules.event.event.EnrollmentAcceptedEvent;
import com.jokahobby.modules.event.event.EnrollmentRejectedEvent;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.hobby.HobbyService;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EventApplicationService {

    private final HobbyService hobbyService;
    private final EventService eventService;
    private final ApplicationEventPublisher eventPublisher;

    public EventResponse createEvent(String path, Account account, EventCreateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Event event = request.toEntity(hobby, account);
        Event saved = eventService.createEvent(event);
        log.info("Event created path={}, eventId={}", path, saved.getId());
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby,
                "'" + saved.getTitle() + "' event created"));
        return EventResponse.from(saved, account);
    }

    @Transactional(readOnly = true)
    public List<EventListResponse> getEvents(String path) {
        Hobby hobby = hobbyService.getHobby(path);
        List<Event> events = eventService.getEventsByHobby(hobby);
        return events.stream().map(EventListResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(String path, Long eventId, Account account) {
        Hobby hobby = hobbyService.getHobby(path);
        Event event = eventService.getEventWithHobbyCheck(eventId, hobby);
        return EventResponse.from(event, account);
    }

    public EventResponse updateEvent(String path, Long eventId, Account account, EventUpdateRequest request) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Event event = eventService.getEventWithHobbyCheckForUpdate(eventId, hobby);

        if (request.limitOfEnrollments() < event.getNumberOfAcceptedEnrollments()) {
            throw new BusinessException(ErrorCode.EVENT_ENROLLMENT_LIMIT_TOO_LOW);
        }

        List<Enrollment> promotedEnrollments = eventService.updateEvent(event, request.title(), request.description(),
                request.endEnrollmentDateTime(), request.startDateTime(),
                request.endDateTime(), request.limitOfEnrollments());

        log.info("Event updated eventId={}", eventId);
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby,
                "'" + event.getTitle() + "' event updated. Please check the details."));
        promotedEnrollments.forEach(enrollment ->
                eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment)));

        return EventResponse.from(event, account);
    }

    public void deleteEvent(String path, Long eventId, Account account) {
        Hobby hobby = hobbyService.getHobbyWithManagerCheck(account, path);
        Event event = eventService.getEventWithHobbyCheck(eventId, hobby);
        eventService.deleteEvent(event);
        log.info("Event deleted eventId={}", eventId);
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby,
                "'" + event.getTitle() + "' event canceled."));
    }

    public void enroll(String path, Long eventId, Account account) {
        Hobby hobby = hobbyService.getHobby(path);
        Event event = eventService.getEventWithHobbyCheckForUpdate(eventId, hobby);

        if (!event.isEnrollableFor(account)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_ENROLLABLE);
        }

        Enrollment enrollment = eventService.newEnrollment(event, account);
        log.info("Enrolled eventId={}", eventId);
        if (enrollment.isAccepted()) {
            eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment));
        }
    }

    public void disenroll(String path, Long eventId, Account account) {
        Hobby hobby = hobbyService.getHobby(path);
        Event event = eventService.getEventWithHobbyCheckForUpdate(eventId, hobby);

        if (!event.isDisenrollableFor(account)) {
            throw new BusinessException(ErrorCode.EVENT_NOT_DISENROLLABLE);
        }

        Enrollment promotedEnrollment = eventService.cancelEnrollment(event, account);
        log.info("Disenrolled eventId={}", eventId);
        if (promotedEnrollment != null) {
            eventPublisher.publishEvent(new EnrollmentAcceptedEvent(promotedEnrollment));
        }
    }

    public void acceptEnrollment(String path, Long enrollmentId, Account account) {
        hobbyService.getHobbyWithManagerCheck(account, path);
        Enrollment enrollment = eventService.getEnrollment(enrollmentId);
        Event event = enrollment.getEvent();

        if (!event.canAccept(enrollment)) {
            throw new BusinessException(ErrorCode.EVENT_CANNOT_ACCEPT);
        }

        eventService.acceptEnrollment(event, enrollment);
        log.info("Enrollment accepted enrollmentId={}", enrollmentId);
        eventPublisher.publishEvent(new EnrollmentAcceptedEvent(enrollment));
    }

    public void rejectEnrollment(String path, Long enrollmentId, Account account) {
        hobbyService.getHobbyWithManagerCheck(account, path);
        Enrollment enrollment = eventService.getEnrollment(enrollmentId);
        Event event = enrollment.getEvent();

        if (!event.canReject(enrollment)) {
            throw new BusinessException(ErrorCode.EVENT_CANNOT_REJECT);
        }

        eventService.rejectEnrollment(event, enrollment);
        log.info("Enrollment rejected enrollmentId={}", enrollmentId);
        eventPublisher.publishEvent(new EnrollmentRejectedEvent(enrollment));
    }

    public void checkIn(String path, Long enrollmentId, Account account) {
        hobbyService.getHobbyWithManagerCheck(account, path);
        Enrollment enrollment = eventService.getEnrollment(enrollmentId);
        eventService.checkInEnrollment(enrollment);
        log.info("Check-in enrollmentId={}", enrollmentId);
    }

    public void cancelCheckIn(String path, Long enrollmentId, Account account) {
        hobbyService.getHobbyWithManagerCheck(account, path);
        Enrollment enrollment = eventService.getEnrollment(enrollmentId);
        eventService.cancelCheckInEnrollment(enrollment);
        log.info("Check-in canceled enrollmentId={}", enrollmentId);
    }
}
