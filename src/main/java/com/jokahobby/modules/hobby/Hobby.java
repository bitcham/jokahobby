package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.account.UserAccount;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.FetchType.*;
import static java.nio.charset.StandardCharsets.UTF_8;

@NamedEntityGraph(name = "Hobby.withAll", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers"),
        @NamedAttributeNode("members")})
@NamedEntityGraph(name = "Hobby.withTagsAndManagers", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("managers")})
@NamedEntityGraph(name = "Hobby.withZonesAndManagers", attributeNodes = {
        @NamedAttributeNode("zones"),
        @NamedAttributeNode("managers")})
@NamedEntityGraph(name = "Hobby.withManagers", attributeNodes = {
        @NamedAttributeNode("managers")})
@NamedEntityGraph(name = "Hobby.withMembers", attributeNodes = {
        @NamedAttributeNode("members")})
@NamedEntityGraph(name = "Hobby.withTagsAndZones", attributeNodes = {
        @NamedAttributeNode("tags"),
        @NamedAttributeNode("zones")})
@NamedEntityGraph(name = "Hobby.withManagersAndMembers", attributeNodes = {
        @NamedAttributeNode("managers"),
        @NamedAttributeNode("members")})
@Entity
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@AllArgsConstructor
@NoArgsConstructor
public class Hobby {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToMany
    private Set<Account> managers = new HashSet<>();

    @ManyToMany
    private Set<Account> members =  new HashSet<>();

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

    @ManyToMany
    private Set<Tag> tags = new HashSet<>();

    @ManyToMany
    private Set<Zone> zones = new HashSet<>();

    private LocalDateTime publishedDateTime;

    private LocalDateTime closedDateTime;

    private LocalDateTime recruitingUpdatedDateTime;

    private boolean recruiting;

    private boolean published;

    private boolean closed;

    private boolean useBanner;

    public void addManager(Account account) {
        this.managers.add(account);
    }

    public void addMemeber(Account account) {
        this.members.add(account);
    }

    public boolean isJoinable(UserAccount userAccount) {
        Account account = userAccount.getAccount();
        return this.isPublished() && this.isRecruiting()
                && !this.members.contains(account) && !this.managers.contains(account);
    }

    public boolean isMember(UserAccount userAccount) {
        return this.members.contains(userAccount.getAccount());
    }

    public boolean isManager(UserAccount userAccount) {
        return this.managers.contains(userAccount.getAccount());
    }

    public String getImage() {
        return image != null ? image : "/images/default_banner.jpg";
    }


    public void publish() {
        if(!this.closed && !this.published) {
            this.published = true;
            this.publishedDateTime = LocalDateTime.now();
        } else{
            throw new RuntimeException("Hobby is already published or closed.");
        }
    }

    public void close() {
        if(this.published && !this.closed) {
            this.closed = true;
            this.closedDateTime = LocalDateTime.now();
        } else{
            throw new RuntimeException("Hobby is already closed or not published.");
        }
    }

    public boolean canUpdateRecruiting() {
        return this.published && this.recruitingUpdatedDateTime == null
                || this.recruitingUpdatedDateTime.isBefore(LocalDateTime.now().minusHours(1));
    }

    public void startRecruit() {
        if(canUpdateRecruiting()) {
            this.recruiting = true;
            this.recruitingUpdatedDateTime = LocalDateTime.now();
        } else {
            throw new RuntimeException("Recruiting cannot be started. Please publish the hobby or try again after one hour.");
        }
    }

    public void stopRecruit() {
        if(canUpdateRecruiting()) {
            this.recruiting = false;
            this.recruitingUpdatedDateTime = LocalDateTime.now();
        } else {
            throw new RuntimeException("Recruiting cannot be stopped. Please publish the hobby or try again after one hour.");
        }
    }

    public boolean isRemovable() {
        return !this.published || this.closed;
    }

    public void addMember(Account account) {
        this.getMembers().add(account);
    }

    public void removeMember(Account account) {
        this.getMembers().remove(account);
    }

    public String getEncodedPath() {
        return URLEncoder.encode(this.path, UTF_8);
    }

    public boolean isManagedBy(Account account) {
        return this.managers.contains(account);
    }
}
