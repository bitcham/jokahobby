package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HobbyMemberRepository extends JpaRepository<HobbyMember, Long> {

    boolean existsByHobbyAndAccount(Hobby hobby, Account account);

    List<HobbyMember> findAllByHobbyId(Long hobbyId);

    List<HobbyMember> findAllByAccountAndHobbyClosedOrderByCreatedAtDesc(Account account, boolean closed);

    void deleteByHobbyAndAccount(Hobby hobby, Account account);
}
