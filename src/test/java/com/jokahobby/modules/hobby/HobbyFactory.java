package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HobbyFactory {

    @Autowired HobbyService HobbyService;
    @Autowired HobbyRepository HobbyRepository;

    public Hobby createHobby(String path, Account manager) {
        Hobby hobby = Hobby.builder().title("test").path(path).build();
        HobbyService.createNewHobby(hobby, manager);
        return hobby;
    }

}
