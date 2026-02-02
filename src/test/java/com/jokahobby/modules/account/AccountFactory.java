package com.jokahobby.modules.account;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AccountFactory {

    @Autowired AccountRepository accountRepository;

    public Account createAccount(String nickname) {
        Account account = Account.builder()
                .nickname(nickname)
                .email(nickname + "@email.com")
                .provider("GOOGLE")
                .providerId("test-" + nickname)
                .joinedAt(LocalDateTime.now())
                .build();
        accountRepository.save(account);
        return account;
    }

}
