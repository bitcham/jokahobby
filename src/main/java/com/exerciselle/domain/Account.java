package com.exerciselle.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter
@Setter @EqualsAndHashCode(of = "id")
@Builder @AllArgsConstructor @NoArgsConstructor
public class Account {

    @Id @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

    private boolean emailVerified;

    private String emailCheckToken;

    private LocalDateTime joinedAt;

    private String bio;

    private String url;

    private String occupation;

    private String location;

    @Lob @Basic(fetch = EAGER)
    private String profileImage;

    private boolean hobbyCreatedByEmail;

    private boolean hobbyCreatedByWeb;

    private boolean hobbyEnrollmentResultByEmail;

    private boolean hobbyUpdatedByEmail;

    private boolean hobbyUpdatedByWeb;
}
