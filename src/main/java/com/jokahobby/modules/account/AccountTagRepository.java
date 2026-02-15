package com.jokahobby.modules.account;

import com.jokahobby.modules.tag.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountTagRepository extends JpaRepository<AccountTag, Long> {

    @Query("SELECT at FROM AccountTag at JOIN FETCH at.tag WHERE at.account.id = :accountId")
    List<AccountTag> findAllByAccountId(UUID accountId);

    Optional<AccountTag> findByAccountAndTag(Account account, Tag tag);

    void deleteByAccountAndTag(Account account, Tag tag);

    boolean existsByAccountAndTag(Account account, Tag tag);
}
