package com.jokahobby.modules.account;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface AccountRepository extends JpaRepository<Account, Long>{
    boolean existsByEmail(@Email @NotBlank String email);

    boolean existsByNickname(@NotBlank String nickname);

    Account findByEmail(String mail);

    Account findByNickname(String nickname);
}
