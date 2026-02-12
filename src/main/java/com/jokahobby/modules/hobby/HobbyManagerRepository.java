package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HobbyManagerRepository extends JpaRepository<HobbyManager, Long> {

    boolean existsByHobbyAndAccount(Hobby hobby, Account account);

    List<HobbyManager> findAllByHobbyId(Long hobbyId);
}
