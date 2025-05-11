package com.jokahobby.hobby;

import com.jokahobby.domain.Hobby;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface HobbyRepository extends JpaRepository<Hobby, Long> {
    boolean existsByPath(@NotBlank @Length(min = 3, max = 20) @Pattern(regexp = "^[a-zA-Z0-9가-힣äöåÄÖÅ]{3,20}$") String path);

    boolean existsByTitle(@NotBlank @Length(max = 50) String title);
}
