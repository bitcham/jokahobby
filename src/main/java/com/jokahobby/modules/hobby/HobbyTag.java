package com.jokahobby.modules.hobby;

import com.jokahobby.modules.tag.Tag;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"hobby_id", "tag_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
public class HobbyTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_id", nullable = false)
    private Hobby hobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private HobbyTag(Hobby hobby, Tag tag) {
        this.hobby = hobby;
        this.tag = tag;
        this.createdAt = LocalDateTime.now();
    }
}
