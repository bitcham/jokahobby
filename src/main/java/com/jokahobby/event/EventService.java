package com.jokahobby.event;

import com.jokahobby.domain.Account;
import com.jokahobby.domain.Event;
import com.jokahobby.domain.Hobby;
import com.jokahobby.event.form.EventForm;
import com.jokahobby.event.validator.EventValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;



    public Event createNewEvent(Event event, Hobby hobby, Account account) {
        event.setCreateBy(account);
        event.setCreatedDateTime(LocalDateTime.now());
        event.setHobby(hobby);
        return eventRepository.save(event);
    }
}
