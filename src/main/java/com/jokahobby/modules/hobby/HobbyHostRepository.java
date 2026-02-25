package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface HobbyHostRepository extends JpaRepository<HobbyHost, Long> {

    boolean existsByHobbyAndAccount(Hobby hobby, Account account);

    @Query("SELECT hh FROM HobbyHost hh JOIN FETCH hh.account WHERE hh.hobby.id = :hobbyId")
    Optional<HobbyHost> findByHobbyId(Long hobbyId);
}
