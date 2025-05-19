package com.jokahobby.modules.hobby.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Async
@Component
@Transactional(readOnly = true)
public class HobbyEventListener {

    @EventListener
    public void handleHobbyCreatedEvent(HobbyCreatedEvent event) {
        log.info("Hobby created: {}", event.getHobby().getTitle());
        //TODO: send email or notification
    }
}
