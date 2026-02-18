package com.jokahobby.modules.hobby;

import com.jokahobby.modules.zone.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HobbyZoneRepository extends JpaRepository<HobbyZone, Long> {

    @Query("SELECT hz FROM HobbyZone hz JOIN FETCH hz.zone WHERE hz.hobby.id = :hobbyId")
    List<HobbyZone> findAllByHobbyId(Long hobbyId);

    Optional<HobbyZone> findByHobbyAndZone(Hobby hobby, Zone zone);

    void deleteByHobbyAndZone(Hobby hobby, Zone zone);

    boolean existsByHobbyAndZone(Hobby hobby, Zone zone);
}
