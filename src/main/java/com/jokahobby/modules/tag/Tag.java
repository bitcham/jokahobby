package com.jokahobby.modules.tag;

import com.jokahobby.modules.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @EqualsAndHashCode(of = "id", callSuper = false)
@Builder @AllArgsConstructor @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String title;
}
