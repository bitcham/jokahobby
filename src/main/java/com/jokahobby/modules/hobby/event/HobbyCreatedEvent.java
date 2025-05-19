package com.jokahobby.modules.hobby.event;

import com.jokahobby.modules.hobby.Hobby;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class HobbyCreatedEvent {

    private Hobby hobby;

    public HobbyCreatedEvent(Hobby newHobby) {
        this.hobby = newHobby;
    }
}
