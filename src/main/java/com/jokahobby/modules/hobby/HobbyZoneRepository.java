package com.jokahobby.modules.hobby;

import com.jokahobby.modules.zone.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HobbyZoneRepository extends JpaRepository<HobbyZone, Long> {

    List<HobbyZone> findAllByHobbyId(Long hobbyId);

    Optional<HobbyZone> findByHobbyAndZone(Hobby hobby, Zone zone);

    void deleteByHobbyAndZone(Hobby hobby, Zone zone);

    boolean existsByHobbyAndZone(Hobby hobby, Zone zone);
}
