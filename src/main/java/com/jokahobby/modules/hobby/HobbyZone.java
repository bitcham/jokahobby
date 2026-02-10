package com.jokahobby.modules.hobby;

import com.jokahobby.modules.common.BaseEntity;
import com.jokahobby.modules.zone.Zone;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"hobby_id", "zone_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id", callSuper = false)
public class HobbyZone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @Builder
    private HobbyZone(Hobby hobby, Zone zone) {
        this.hobby = hobby;
        this.zone = zone;
    }
}
