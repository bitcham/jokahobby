package com.jokahobby.modules.hobby;

import com.jokahobby.modules.common.BaseEntity;
import com.jokahobby.modules.tag.Tag;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"hobby_id", "tag_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id", callSuper = false)
public class HobbyTag extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    private HobbyTag(Hobby hobby, Tag tag) {
        this.hobby = hobby;
        this.tag = tag;
    }
}
