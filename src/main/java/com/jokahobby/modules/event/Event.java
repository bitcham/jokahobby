package com.jokahobby.modules.event;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.common.SoftDeletableEntity;
import com.jokahobby.modules.hobby.Hobby;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NamedEntityGraph(
        name = "Event.withEnrollments",
        attributeNodes = @NamedAttributeNode("enrollments")
)
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter @Setter @EqualsAndHashCode(of = "id", callSuper = false)
public class Event extends SoftDeletableEntity {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    private Hobby hobby;

    @ManyToOne
    private Account createdBy;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Column(nullable = false)
    private LocalDateTime createDateTime;

    @Column(nullable = false)
    private LocalDateTime endEnrollmentDateTime;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Column
    private Integer limitOfEnrollments;

    @OneToMany(mappedBy = "event")
    @OrderBy("enrolledAt")
    private List<Enrollment> enrollments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    public void setCreatedDateTime(LocalDateTime now) {
        this.createDateTime = now;
    }

    private boolean isNotClosed() {
        return this.endEnrollmentDateTime.isAfter(LocalDateTime.now());
    }

    public boolean isEnrollableFor(Account account) {
        return isNotClosed() && !isAttended(account) && !isAlreadyEnrolled(account);
    }

    public boolean isDisenrollableFor(Account account) {
        return isNotClosed() && !isAttended(account) && isAlreadyEnrolled(account);
    }

    public boolean isAttended(Account account) {
        for (Enrollment e : this.enrollments) {
            if (e.getAccount().equals(account) && e.isAttended()) {
                return true;
            }
        }

        return false;
    }

    private boolean isAlreadyEnrolled(Account account) {
        for (Enrollment e : this.enrollments) {
            if (e.getAccount().equals(account)) {
                return true;
            }
        }
        return false;
    }

    public int numberOfRemainSpots(){
        return this.limitOfEnrollments - (int) this.enrollments.stream()
                .filter(Enrollment::isAccepted)
                .count();
    }

    public long getNumberOfAcceptedEnrollments() {
        return this.enrollments.stream().filter(Enrollment::isAccepted).count();
    }

    public boolean isAbleToAcceptWaitingEnrollment() {
        return this.eventType == EventType.FCFS && this.getNumberOfAcceptedEnrollments() < this.limitOfEnrollments;
    }

    public void addEnrollment(Enrollment enrollment) {
        this.enrollments.add(enrollment);
        enrollment.setEvent(this);
    }

    public void removeEnrollment(Enrollment enrollment) {
        this.enrollments.remove(enrollment);
        enrollment.setEvent(null);
    }

    public void acceptNextWaitingEnrollment() {
        if(this.isAbleToAcceptWaitingEnrollment()) {
            Enrollment enrollmentToAccept = this.getTheFirstWaitingEnrollment();
            if(enrollmentToAccept != null) {
                enrollmentToAccept.setAccepted(true);
            }
        }
    }

    private Enrollment getTheFirstWaitingEnrollment() {
        for (Enrollment enrollment : this.enrollments) {
            if (!enrollment.isAccepted()) {
                return enrollment;
            }
        }
        return null;
    }

    private List<Enrollment> getWaitingList() {
        return this.enrollments.stream().filter(enrollment -> !enrollment.isAccepted()).toList();
    }

    public void acceptWaitingList() {
        if(this.isAbleToAcceptWaitingEnrollment()){
            var waitingList = getWaitingList();
            int numberToAccept = (int) Math.min(this.limitOfEnrollments - this.getNumberOfAcceptedEnrollments(), waitingList.size());
            waitingList.subList(0, numberToAccept).forEach(enrollment -> {
                enrollment.setAccepted(true);
            });
        }
    }

    public boolean canAccept(Enrollment enrollment) {
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && !enrollment.isAttended()
                && !enrollment.isAccepted();
    }

    public boolean canReject(Enrollment enrollment) {
        return this.eventType == EventType.CONFIRMATIVE
                && this.enrollments.contains(enrollment)
                && !enrollment.isAttended()
                && enrollment.isAccepted();
    }

    public void accept(Enrollment enrollment) {
        if(this.eventType == EventType.CONFIRMATIVE && this.limitOfEnrollments > this.getNumberOfAcceptedEnrollments()) {
            enrollment.setAccepted(true);
        }
    }

    public void reject(Enrollment enrollment) {
        if(this.eventType == EventType.CONFIRMATIVE){
            enrollment.setAccepted(false);
        }
    }
}
