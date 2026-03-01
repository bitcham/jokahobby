package com.jokahobby.modules.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Transactional(readOnly = true)
public interface AccountRepository extends JpaRepository<Account, UUID>, QuerydslPredicateExecutor<Account> {

    boolean existsByNickname(String nickname);

    Optional<Account> findByNickname(String nickname);

    Optional<Account> findByProviderAndProviderId(String provider, String providerId);
}
