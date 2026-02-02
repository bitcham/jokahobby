package com.jokahobby.modules.account;

import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.FetchType.*;

@Entity
@Getter @Setter @EqualsAndHashCode(of = "id")
@Builder @AllArgsConstructor @NoArgsConstructor
public class Account {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;

    @Column(unique = true)
    private String nickname;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    private LocalDateTime joinedAt;

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

    @ManyToMany
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany
    private Set<Zone> zones = new HashSet<>();

}
