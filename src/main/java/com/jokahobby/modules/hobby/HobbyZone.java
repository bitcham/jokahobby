package com.jokahobby.modules.hobby;

import com.jokahobby.modules.zone.Zone;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"hobby_id", "zone_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class HobbyZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private HobbyZone(Hobby hobby, Zone zone) {
        this.hobby = hobby;
        this.zone = zone;
        this.createdAt = LocalDateTime.now();
    }
}
