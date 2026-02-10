package com.jokahobby.modules.account;

import com.jokahobby.modules.common.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.FetchType.*;

@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter @Setter @EqualsAndHashCode(of = "id", callSuper = false)
@Builder @AllArgsConstructor @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends SoftDeletableEntity {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;

    private String nickname;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    private Instant joinedAt;

    private String bio;

    private String url;

    private String location;

    @Lob @Basic(fetch = EAGER)
    private String profileImage;

    private boolean hobbyCreatedByEmail;

    private boolean hobbyCreatedByWeb = true;

    private boolean hobbyEnrollmentResultByEmail;

    private boolean hobbyEnrollmentResultByWeb = true;

    private boolean hobbyUpdatedByEmail;

    private boolean hobbyUpdatedByWeb = true;

}
