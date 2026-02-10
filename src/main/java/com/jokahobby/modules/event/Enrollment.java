package com.jokahobby.modules.event;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.common.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

@NamedEntityGraph(
        name = "Enrollment.withEventAndHobby",
        attributeNodes = {
                @NamedAttributeNode(value = "event", subgraph = "hobby")
        },
        subgraphs = @NamedSubgraph(name = "hobby", attributeNodes = @NamedAttributeNode("hobby"))
)
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter @Setter @EqualsAndHashCode(of = "id", callSuper = false)
public class Enrollment extends SoftDeletableEntity {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private Event event;

    @ManyToOne
    private Account account;

    private Instant enrolledAt;

    private boolean accepted;

    private boolean attended;


}
