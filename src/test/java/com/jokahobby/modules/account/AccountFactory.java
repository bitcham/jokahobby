package com.jokahobby.modules.account;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountFactory {

    @Autowired AccountRepository accountRepository;

    public Account createAccount(String nickname) {
        Account cutedog = new Account();
        cutedog.setNickname(nickname);
        cutedog.setEmail(nickname + "@email.com");
        accountRepository.save(cutedog);
        return cutedog;
    }

}
