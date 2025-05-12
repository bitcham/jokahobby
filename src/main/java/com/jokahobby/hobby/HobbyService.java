package com.jokahobby.hobby;

import com.jokahobby.domain.Account;
import com.jokahobby.domain.Hobby;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HobbyService {

    private final HobbyRepository hobbyRepository;

    public Hobby createNewHobby(Hobby hobby, Account account) {
        Hobby newHobby = hobbyRepository.save(hobby);
        newHobby.addManager(account);
        return newHobby;
    }

    public Hobby getHobby(String path) {
        Hobby hobby = hobbyRepository.findByPath(path);
        if (hobby == null) {
            throw new IllegalArgumentException("Hobby not found");
        }
        return hobby;
    }
}
