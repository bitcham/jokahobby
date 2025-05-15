package com.jokahobby.event;

import com.jokahobby.account.CurrentAccount;
import com.jokahobby.domain.Account;
import com.jokahobby.domain.Event;
import com.jokahobby.domain.Hobby;
import com.jokahobby.event.form.EventForm;
import com.jokahobby.event.validator.EventValidator;
import com.jokahobby.hobby.HobbyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/hobby/{path}")
@RequiredArgsConstructor
public class EventController {

    private final HobbyService hobbyService;
    private final EventService eventService;
    private final ModelMapper modelMapper;

    @InitBinder("eventForm")
    public void initBinder(WebDataBinder webDataBinder) {
        webDataBinder.setValidator(new EventValidator());
    }

    @GetMapping("/events")
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
        Event event = eventService.createNewEvent(modelMapper.map(eventForm, Event.class), hobby, account);
        return "redirect:/hobby/" + hobby.getEncodedPath() + "/events/" + event.getId();
    }


}
