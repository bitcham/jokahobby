package com.jokahobby.modules.event;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.common.SoftDeletableEntity;
import com.jokahobby.modules.hobby.Hobby;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@NamedEntityGraph(
        name = "Event.withEnrollments",
        attributeNodes = @NamedAttributeNode("enrollments")
)
@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter @EqualsAndHashCode(of = "id", callSuper = false)
@Builder @AllArgsConstructor @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends SoftDeletableEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Hobby hobby;

    @ManyToOne
    private Account createdBy;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Instant createDateTime = Instant.now();

    @Column(nullable = false)
    private Instant endEnrollmentDateTime;

    @Column(nullable = false)
    private Instant startDateTime;

    @Column(nullable = false)
    private Instant endDateTime;

    @Column
    private Integer limitOfEnrollments;

    @Builder.Default
    @OneToMany(mappedBy = "event")
    @OrderBy("enrolledAt")
    private List<Enrollment> enrollments = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    public void updateDetails(String title, String description, Instant endEnrollmentDateTime,
                              Instant startDateTime, Instant endDateTime, Integer limitOfEnrollments) {
        this.title = title;
        this.description = description;
        this.endEnrollmentDateTime = endEnrollmentDateTime;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.limitOfEnrollments = limitOfEnrollments;
    }

    private boolean isUnlimited() {
        return this.limitOfEnrollments == null;
    }

    private boolean isNotClosed() {
        return this.endEnrollmentDateTime.isAfter(Instant.now());
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
        if (isUnlimited()) {
            return Integer.MAX_VALUE;
        }
        return this.limitOfEnrollments - (int) this.getNumberOfAcceptedEnrollments();
    }

    public long getNumberOfAcceptedEnrollments() {
        return this.enrollments.stream().filter(Enrollment::isAccepted).count();
    }

    private boolean hasCapacityAvailable() {
        return isUnlimited() || this.getNumberOfAcceptedEnrollments() < this.limitOfEnrollments;
    }

    public boolean isAbleToAcceptWaitingEnrollment() {
        return this.eventType == EventType.FCFS && hasCapacityAvailable();
    }

    public void addEnrollment(Enrollment enrollment) {
        this.enrollments.add(enrollment);
        enrollment.assignEvent(this);
    }

    public void removeEnrollment(Enrollment enrollment) {
        this.enrollments.remove(enrollment);
        enrollment.unassignEvent();
    }

    public Enrollment acceptNextWaitingEnrollment() {
        if (this.isAbleToAcceptWaitingEnrollment()) {
            Enrollment enrollmentToAccept = this.getTheFirstWaitingEnrollment();
            if (enrollmentToAccept != null) {
                enrollmentToAccept.accept();
                return enrollmentToAccept;
            }
        }
        return null;
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

    public List<Enrollment> acceptWaitingList() {
        if (this.isAbleToAcceptWaitingEnrollment()) {
            var waitingList = getWaitingList();
            if (isUnlimited()) {
                waitingList.forEach(Enrollment::accept);
                return waitingList;
            }
            int numberToAccept = (int) Math.min(
                    this.limitOfEnrollments - this.getNumberOfAcceptedEnrollments(), waitingList.size());
            List<Enrollment> toAccept = waitingList.subList(0, numberToAccept);
            toAccept.forEach(Enrollment::accept);
            return toAccept;
        }
        return List.of();
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

    private boolean hasRemainingCapacity() {
        return this.eventType == EventType.CONFIRMATIVE && hasCapacityAvailable();
    }

    public void accept(Enrollment enrollment) {
        if (!hasRemainingCapacity()) {
            throw new BusinessException(ErrorCode.EVENT_CANNOT_ACCEPT);
        }
        enrollment.accept();
    }

    public void reject(Enrollment enrollment) {
        if (this.eventType != EventType.CONFIRMATIVE) {
            throw new BusinessException(ErrorCode.EVENT_CANNOT_REJECT);
        }
        enrollment.reject();
    }
}
