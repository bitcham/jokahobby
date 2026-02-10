package com.jokahobby.modules.tag;

import com.jokahobby.modules.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter @Setter @EqualsAndHashCode(of = "id", callSuper = false)
@Builder @AllArgsConstructor @NoArgsConstructor
public class Tag extends BaseEntity {

    @Id @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private String title;
}
