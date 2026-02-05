package com.jokahobby.modules.account;

import com.jokahobby.modules.zone.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountZoneRepository extends JpaRepository<AccountZone, Long> {

    List<AccountZone> findAllByAccountId(UUID accountId);

    Optional<AccountZone> findByAccountAndZone(Account account, Zone zone);

    void deleteByAccountAndZone(Account account, Zone zone);

    boolean existsByAccountAndZone(Account account, Zone zone);
}
