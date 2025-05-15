package com.jokahobby.event;

import com.jokahobby.account.CurrentAccount;
import com.jokahobby.domain.Account;
import com.jokahobby.domain.Hobby;
import com.jokahobby.event.form.EventForm;
import com.jokahobby.hobby.HobbyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hobby/{path}")
@RequiredArgsConstructor
public class EventController {

    private final HobbyService hobbyService;

    @GetMapping("/events")
    public String newEventForm(@CurrentAccount Account account, @PathVariable String path, Model model){
        Hobby hobby = hobbyService.getHobbyToUpdateStatus(account, path);
        model.addAttribute(hobby);
        model.addAttribute(account);
        model.addAttribute(new EventForm());
        return "event/form";
    }
}
