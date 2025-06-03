package com.jokahobby.modules.event;

import com.jokahobby.modules.account.Account;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@NamedEntityGraph(
        name = "Enrollment.withEventAndHobby",
        attributeNodes = {
                @NamedAttributeNode(value = "event", subgraph = "hobby")
        },
        subgraphs = @NamedSubgraph(name = "hobby", attributeNodes = @NamedAttributeNode("hobby"))
)
@Entity
@Getter @Setter @EqualsAndHashCode(of = "id")
public class Enrollment {

    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private Event event;

    @ManyToOne
    private Account account;

    private LocalDateTime enrolledAt;

    private boolean accepted;

    private boolean attended;


}
