package com.jokahobby.modules.event;

import com.jokahobby.modules.account.CurrentAccount;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.Hobby;
import com.jokahobby.modules.event.form.EventForm;
import com.jokahobby.modules.event.validator.EventValidator;
import com.jokahobby.modules.hobby.HobbyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/hobby/{path}")
@RequiredArgsConstructor
public class EventController {

    private final HobbyService hobbyService;
    private final EventService eventService;
    private final ModelMapper modelMapper;
    private final EventRepository eventRepository;

    @InitBinder("eventForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.setValidator(new EventValidator());
    }

    @GetMapping("/new-event")
    public String newEventForm(@CurrentAccount Account account, @PathVariable String path, Model model){
        Hobby hobby = hobbyService.getHobbyToUpdateStatus(account, path);
        model.addAttribute(hobby);
        model.addAttribute(account);
        model.addAttribute(new EventForm());
        return "event/form";
    }

    @PostMapping("/new-event")
    public String newEventSubmit(@CurrentAccount Account account, @PathVariable String path,
                                 @Valid EventForm eventForm, Errors errors, Model model) {
        Hobby hobby = hobbyService.getHobbyToUpdateStatus(account, path);
        if(errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(hobby);
            return "event/form";
        }
        Event event = eventService.createEvent(modelMapper.map(eventForm, Event.class), hobby, account);
        return "redirect:/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{id}")
    public String getEvent(@CurrentAccount Account account, @PathVariable String path,
                           @PathVariable("id") Event event, Model model) {
        model.addAttribute(account);
        model.addAttribute(event);
        model.addAttribute(hobbyService.getHobby(path));
        return "event/view";
    }

    @GetMapping("/events")
    public String viewHobbyEvents(@CurrentAccount Account account, @PathVariable String path, Model model) {
        Hobby hobby = hobbyService.getHobby(path);
        model.addAttribute(account);
        model.addAttribute(hobby);

        List<Event> events = eventRepository.findByHobbyOrderByStartDateTime(hobby);
        List<Event> newEvents = new ArrayList<>();
        List<Event> oldEvents = new ArrayList<>();
        events.forEach(e -> {
            if (e.getEndDateTime().isBefore(LocalDateTime.now())){
                oldEvents.add(e);
            } else {
                newEvents.add(e);
            }
        });

        model.addAttribute("newEvents", newEvents);
        model.addAttribute("oldEvents", oldEvents);
        return "hobby/events";
    }

    @GetMapping("/events/{id}/edit")
    public String updateEventForm(@CurrentAccount Account account, @PathVariable String path,
                                  @PathVariable("id") Event event, Model model) {
        Hobby hobby = hobbyService.getHobbyToUpdate(account, path);
        model.addAttribute(account);
        model.addAttribute(hobby);
        model.addAttribute(event);
        model.addAttribute(modelMapper.map(event, EventForm.class));
        return "event/update-form";
    }

    @PostMapping("/events/{id}/edit")
    public String updateEventSubmit(@CurrentAccount Account account, @PathVariable String path,
                                    @PathVariable("id") Event event, @Valid EventForm eventForm, Errors errors, Model model) {
        Hobby hobby = hobbyService.getHobbyToUpdate(account, path);
        eventForm.setEventType(event.getEventType());

        if (eventForm.getLimitOfEnrollments() < event.getNumberOfAcceptedEnrollments()) {
            errors.rejectValue("limitOfEnrollments", "invalid.limitOfEnrollments", "Limit of enrollments must be at least " + event.getNumberOfAcceptedEnrollments() + ".");
        }

        if(errors.hasErrors()) {
            model.addAttribute(account);
            model.addAttribute(hobby);
            model.addAttribute(event);
            return "event/update-form";
        }

        eventService.updateEvent(event, eventForm);
        return "redirect:/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId();
    }

    @DeleteMapping("/events/{id}")
    public String cancelEvent(@CurrentAccount Account account, @PathVariable String path,
                              @PathVariable("id") Event event) {
        Hobby hobby = hobbyService.getHobbyToUpdateStatus(account, path);
        eventService.deleteEvent(event);
        return "redirect:/hobby/" + hobby.getEncodedPath() + "/events";
    }

    @PostMapping("/events/{id}/enroll")
    public String newEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                @PathVariable("id") Event event) {
        Hobby hobby = hobbyService.getHobbyToEnroll(path);
        eventService.newEnrollment(event, account);
        return "redirect:/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId();
    }

    @PostMapping("/events/{id}/disenroll")
    public String cancelEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                    @PathVariable("id") Event event) {
        Hobby hobby = hobbyService.getHobbyToEnroll(path);
        eventService.cancelEnrollment(event, account);
        return "redirect:/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/accept")
    public String acceptEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                    @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment){
        Hobby hobby = hobbyService.getHobbyToUpdate(account, path);
        eventService.acceptEnrollment(event, enrollment);
        return "redirect:/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/reject")
    public String rejectEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                    @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment){
        Hobby hobby = hobbyService.getHobbyToUpdate(account, path);
        eventService.rejectEnrollment(event, enrollment);
        return "redirect:/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/checkin")
    public String checkInEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                     @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment){
        Hobby hobby = hobbyService.getHobbyToUpdate(account, path);
        eventService.checkInEnrollment(enrollment);
        return "redirect:/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId();
    }

    @GetMapping("/events/{eventId}/enrollments/{enrollmentId}/cancel-checkin")
    public String cancelCheckInEnrollment(@CurrentAccount Account account, @PathVariable String path,
                                           @PathVariable("eventId") Event event, @PathVariable("enrollmentId") Enrollment enrollment) {
        Hobby hobby = hobbyService.getHobbyToUpdate(account, path);
        eventService.cancelCheckInEnrollment(enrollment);
        return "redirect:/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId();
    }



}
