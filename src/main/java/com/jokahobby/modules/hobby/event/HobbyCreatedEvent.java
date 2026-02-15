package com.jokahobby.modules.hobby.event;

import com.jokahobby.modules.hobby.Hobby;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class HobbyCreatedEvent {

    private final Hobby hobby;

}
