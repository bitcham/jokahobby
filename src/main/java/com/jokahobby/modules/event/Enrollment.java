package com.jokahobby.modules.event;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.common.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
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
@Getter @EqualsAndHashCode(of = "id", callSuper = false)
@Builder @AllArgsConstructor @NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public void accept() {
        this.accepted = true;
    }

    public void reject() {
        this.accepted = false;
    }

    public void checkIn() {
        this.attended = true;
    }

    public void cancelCheckIn() {
        this.attended = false;
    }

    void assignEvent(Event event) {
        this.event = event;
    }

    void unassignEvent() {
        this.event = null;
    }
}
