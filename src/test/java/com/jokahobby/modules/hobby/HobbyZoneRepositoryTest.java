package com.jokahobby.modules.hobby;

import com.jokahobby.infra.AbstractContainerBaseTest;
import com.jokahobby.infra.MockMvcTest;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.AccountRepository;
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
class HobbyZoneRepositoryTest extends AbstractContainerBaseTest {

    @Autowired HobbyZoneRepository hobbyZoneRepository;
    @Autowired HobbyRepository hobbyRepository;
    @Autowired ZoneRepository zoneRepository;
    @Autowired AccountRepository accountRepository;

    private Hobby hobby;
    private Zone zone;

    @BeforeEach
    void setUp() {
        hobbyZoneRepository.deleteAll();
        accountRepository.save(Account.builder()
                .email("manager@example.com")
                .nickname("manager")
                .provider("google")
                .providerId("google-mgr")
                .joinedAt(Instant.now())
                .build());
        hobby = hobbyRepository.save(Hobby.builder()
                .path("test-hobby")
                .title("Test Hobby")
                .shortDescription("desc")
                .build());
        zone = zoneRepository.findByCityAndProvince("Seoul", "none")
                .orElseGet(() -> zoneRepository.save(Zone.builder()
                        .country("Korea")
                        .city("Seoul")
                        .localNameOfCity("서울")
                        .province("none")
                        .build()));
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("persists HobbyZone with audit columns")
        void savesWithAudit() {
            HobbyZone hobbyZone = hobbyZoneRepository.save(HobbyZone.builder()
                    .hobby(hobby)
                    .zone(zone)
                    .build());

            assertThat(hobbyZone.getId()).isNotNull();
            assertThat(hobbyZone.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("rejects duplicate hobby-zone pair")
        void rejectsDuplicate() {
            hobbyZoneRepository.save(HobbyZone.builder()
                    .hobby(hobby).zone(zone).build());

            assertThatThrownBy(() -> {
                hobbyZoneRepository.saveAndFlush(HobbyZone.builder()
                        .hobby(hobby).zone(zone).build());
            }).isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("findAllByHobbyId")
    class FindAllByHobbyId {

        @Test
        @DisplayName("returns all zones for hobby with fetched zone data")
        void returnsAllWithFetchedData() {
            Zone zone2 = zoneRepository.findByCityAndProvince("Busan", "none")
                    .orElseGet(() -> zoneRepository.save(Zone.builder()
                            .country("Korea")
                            .city("Busan")
                            .localNameOfCity("부산")
                            .province("none")
                            .build()));
            hobbyZoneRepository.save(HobbyZone.builder()
                    .hobby(hobby).zone(zone).build());
            hobbyZoneRepository.save(HobbyZone.builder()
                    .hobby(hobby).zone(zone2).build());

            List<HobbyZone> result = hobbyZoneRepository.findAllByHobbyId(hobby.getId());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getZone().getCity()).isNotNull();
        }
    }

    @Nested
    @DisplayName("deleteByHobbyAndZone")
    class DeleteByHobbyAndZone {

        @Test
        @DisplayName("deletes the matching row")
        void deletes() {
            hobbyZoneRepository.save(HobbyZone.builder()
                    .hobby(hobby).zone(zone).build());

            hobbyZoneRepository.deleteByHobbyAndZone(hobby, zone);

            assertThat(hobbyZoneRepository.findByHobbyAndZone(hobby, zone)).isEmpty();
        }
    }
}
