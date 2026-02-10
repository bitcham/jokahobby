package com.jokahobby.modules.account;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.modules.zone.Zone;
import com.jokahobby.modules.zone.ZoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@MockMvcTest
class AccountZoneRepositoryTest extends AbstractContainerBaseTest {

    @Autowired AccountZoneRepository accountZoneRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired ZoneRepository zoneRepository;

    private Account account;
    private Zone zone;

    @BeforeEach
    void setUp() {
        accountZoneRepository.deleteAll();
        account = accountRepository.save(Account.builder()
                .email("test@example.com")
                .nickname("tester")
                .provider("google")
                .providerId("google-1")
                .joinedAt(Instant.now())
                .build());
        zone = zoneRepository.findByCityAndProvince("Seoul", "none")
                .orElseGet(() -> zoneRepository.save(Zone.builder()
                        .country("Korea").city("Seoul").localNameOfCity("서울").province("none").build()));
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persists AccountZone with audit columns")
        void savesWithAudit() {
            AccountZone accountZone = accountZoneRepository.save(AccountZone.builder()
                    .account(account)
                    .zone(zone)
                                        .build());

            assertThat(accountZone.getId()).isNotNull();
            assertThat(accountZone.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("rejects duplicate account-zone pair")
        void rejectsDuplicate() {
            accountZoneRepository.save(AccountZone.builder()
                    .account(account).zone(zone).build());

            assertThatThrownBy(() -> {
                accountZoneRepository.saveAndFlush(AccountZone.builder()
                        .account(account).zone(zone).build());
            }).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("findAllByAccountId")
    class FindAllByAccountId {

        @Test
        @DisplayName("returns all zones for account")
        void returnsAll() {
            Zone zone2 = zoneRepository.findByCityAndProvince("Busan", "none")
                    .orElseGet(() -> zoneRepository.save(Zone.builder()
                            .country("Korea").city("Busan").localNameOfCity("부산").province("none").build()));
            accountZoneRepository.save(AccountZone.builder()
                    .account(account).zone(zone).build());
            accountZoneRepository.save(AccountZone.builder()
                    .account(account).zone(zone2).build());

            List<AccountZone> result = accountZoneRepository.findAllByAccountId(account.getId());

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("deleteByAccountAndZone")
    class DeleteByAccountAndZone {

        @Test
        @DisplayName("deletes the matching row")
        void deletes() {
            accountZoneRepository.save(AccountZone.builder()
                    .account(account).zone(zone).build());

            accountZoneRepository.deleteByAccountAndZone(account, zone);

            assertThat(accountZoneRepository.findByAccountAndZone(account, zone)).isEmpty();
        }
    }
}
