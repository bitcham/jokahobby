package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HobbyMemberRepository extends JpaRepository<HobbyMember, Long> {

    boolean existsByHobbyAndAccount(Hobby hobby, Account account);

    @Query("SELECT hm FROM HobbyMember hm JOIN FETCH hm.account WHERE hm.hobby.id = :hobbyId")
    List<HobbyMember> findAllByHobbyId(Long hobbyId);

    void deleteByHobbyAndAccount(Hobby hobby, Account account);
}
