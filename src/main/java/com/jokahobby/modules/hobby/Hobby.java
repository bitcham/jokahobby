package com.jokahobby.modules.hobby;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

import java.net.URLEncoder;
import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.EAGER;
import static java.nio.charset.StandardCharsets.UTF_8;

@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Hobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String path;

    @Column(unique = true, nullable = false)
    private String title;

    private String shortDescription;

    @Lob
    @Basic(fetch = EAGER)
    private String fullDescription;

    @Lob
    @Basic(fetch = EAGER)
    private String image;

    private LocalDateTime publishedDateTime;

    private LocalDateTime closedDateTime;

    private LocalDateTime recruitingUpdatedDateTime;

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
            this.publishedDateTime = LocalDateTime.now();
        } else {
            throw new BusinessException(ErrorCode.HOBBY_ALREADY_PUBLISHED);
        }
    }

    public void close() {
        if(this.published && !this.closed) {
            this.closed = true;
            this.closedDateTime = LocalDateTime.now();
        } else {
            throw new BusinessException(ErrorCode.HOBBY_NOT_PUBLISHED);
        }
    }

    public boolean canUpdateRecruiting() {
        return this.published && (this.recruitingUpdatedDateTime == null
                || this.recruitingUpdatedDateTime.isBefore(LocalDateTime.now().minusHours(1)));
    }

    public void startRecruit() {
        if(canUpdateRecruiting()) {
            this.recruiting = true;
            this.recruitingUpdatedDateTime = LocalDateTime.now();
        } else {
            throw new BusinessException(ErrorCode.HOBBY_RECRUIT_COOLDOWN);
        }
    }

    public void stopRecruit() {
        if(canUpdateRecruiting()) {
            this.recruiting = false;
            this.recruitingUpdatedDateTime = LocalDateTime.now();
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
