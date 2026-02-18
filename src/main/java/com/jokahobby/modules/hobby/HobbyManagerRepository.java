package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HobbyManagerRepository extends JpaRepository<HobbyManager, Long> {

    boolean existsByHobbyAndAccount(Hobby hobby, Account account);

    @Query("SELECT hm FROM HobbyManager hm JOIN FETCH hm.account WHERE hm.hobby.id = :hobbyId")
    List<HobbyManager> findAllByHobbyId(Long hobbyId);
}
