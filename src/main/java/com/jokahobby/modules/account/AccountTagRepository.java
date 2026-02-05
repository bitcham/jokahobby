package com.jokahobby.modules.account;

import com.jokahobby.modules.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountTagRepository extends JpaRepository<AccountTag, Long> {

    List<AccountTag> findAllByAccountId(UUID accountId);

    Optional<AccountTag> findByAccountAndTag(Account account, Tag tag);

    void deleteByAccountAndTag(Account account, Tag tag);

    boolean existsByAccountAndTag(Account account, Tag tag);
}
