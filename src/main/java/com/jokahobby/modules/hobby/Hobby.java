package com.jokahobby.modules.hobby;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.common.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;

import static jakarta.persistence.FetchType.EAGER;
import static java.nio.charset.StandardCharsets.UTF_8;

@Entity
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@EqualsAndHashCode(of = "id", callSuper = false)
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hobby extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String path;

    @Column(nullable = false)
    private String title;

    private String shortDescription;

    @Lob
    @Basic(fetch = EAGER)
    private String fullDescription;

    @Lob
    @Basic(fetch = EAGER)
    private String image;

    private Instant publishedDateTime;

    private Instant closedDateTime;

    private Instant recruitingUpdatedDateTime;

    private boolean recruiting;

    private boolean published;

    private boolean closed;

    private boolean useBanner;

    private int memberCount;


    public void incrementMemberCount() {
        this.memberCount++;
    }

    public void decrementMemberCount() {
        this.memberCount--;
    }

    public String getImage() {
        return image != null ? image : "/images/default_banner.jpg";
    }


    public void publish() {
        if(!this.closed && !this.published) {
            this.published = true;
            this.publishedDateTime = Instant.now();
        } else {
            throw new BusinessException(ErrorCode.HOBBY_ALREADY_PUBLISHED);
        }
    }

    public void close() {
        if(this.published && !this.closed) {
            this.closed = true;
            this.closedDateTime = Instant.now();
        } else {
            throw new BusinessException(ErrorCode.HOBBY_NOT_PUBLISHED);
        }
    }

    public boolean canUpdateRecruiting() {
        Instant now = Instant.now();
        return this.published && (this.recruitingUpdatedDateTime == null
                || this.recruitingUpdatedDateTime.isBefore(now.minus(Duration.ofHours(1))));
    }

    public void startRecruit() {
        if(canUpdateRecruiting()) {
            this.recruiting = true;
            this.recruitingUpdatedDateTime = Instant.now();
        } else {
            throw new BusinessException(ErrorCode.HOBBY_RECRUIT_COOLDOWN);
        }
    }

    public void stopRecruit() {
        if(canUpdateRecruiting()) {
            this.recruiting = false;
            this.recruitingUpdatedDateTime = Instant.now();
        } else {
            throw new BusinessException(ErrorCode.HOBBY_RECRUIT_COOLDOWN);
        }
    }

    public boolean isRemovable() {
        return !this.published || this.closed;
    }

    public String getEncodedPath() {
        return URLEncoder.encode(this.path, UTF_8);
    }

}
