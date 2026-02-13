package com.jokahobby.modules.hobby;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.event.HobbyCreatedEvent;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.jokahobby.modules.hobby.Hobby.VALID_PATH_PATTERN;

@Service
@RequiredArgsConstructor
public class HobbyService {

    private final HobbyRepository hobbyRepository;
    private final HobbyTagRepository hobbyTagRepository;
    private final HobbyZoneRepository hobbyZoneRepository;
    private final HobbyManagerRepository hobbyManagerRepository;
    private final HobbyMemberRepository hobbyMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Hobby createNewHobby(Hobby hobby, Account account) {
        if (hobbyRepository.existsByPath(hobby.getPath())) {
            throw new BusinessException(ErrorCode.HOBBY_PATH_ALREADY_EXISTS);
        }
        if (hobbyRepository.existsByTitle(hobby.getTitle())) {
            throw new BusinessException(ErrorCode.HOBBY_TITLE_ALREADY_EXISTS);
        }
        Hobby saved = hobbyRepository.save(hobby);
        addManager(saved, account);
        return saved;
    }

    public Page<Hobby> findPublished(String country, String city, HobbySortType sort, Pageable pageable) {
        return hobbyRepository.findPublished(country, city, sort, pageable);
    }

    public Page<Hobby> findByKeyword(String keyword, Pageable pageable) {
        return hobbyRepository.findByKeyword(keyword, pageable);
    }

    public Hobby getHobby(String path) {
        Hobby hobby = this.hobbyRepository.findByPath(path);
        checkIfExistingHobby(path, hobby);
        return hobby;
    }

    public Hobby getHobbyWithManagerCheck(Account account, String path) {
        Hobby hobby = this.getHobby(path);
        checkIfManager(account, hobby);
        return hobby;
    }

    public void updateHobbyDescription(Hobby hobby, String shortDescription, String fullDescription) {
       hobby.updateDescription(shortDescription, fullDescription);
       eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby description updated"));
    }

    public void updateHobbyImage(Hobby hobby, String image) {
        hobby.updateImage(image);
    }

    public void enableHobbyBanner(Hobby hobby) {
        hobby.enableBanner();
    }

    public void disableHobbyBanner(Hobby hobby) {
        hobby.disableBanner();
    }

    public void addTag(Hobby hobby, Tag tag) {
        if (hobbyTagRepository.existsByHobbyAndTag(hobby, tag)) {
            return;
        }
        hobbyTagRepository.save(HobbyTag.builder().hobby(hobby).tag(tag).build());
    }

    public void removeTag(Hobby hobby, Tag tag) {
        hobbyTagRepository.deleteByHobbyAndTag(hobby, tag);
    }

    public List<Tag> getTags(Hobby hobby) {
        return hobbyTagRepository.findAllByHobbyId(hobby.getId()).stream()
                .map(HobbyTag::getTag)
                .toList();
    }

    public void addZone(Hobby hobby, Zone zone) {
        if (hobbyZoneRepository.existsByHobbyAndZone(hobby, zone)) {
            return;
        }
        hobbyZoneRepository.save(HobbyZone.builder().hobby(hobby).zone(zone).build());
    }

    public void removeZone(Hobby hobby, Zone zone) {
        hobbyZoneRepository.deleteByHobbyAndZone(hobby, zone);
    }

    public List<Zone> getZones(Hobby hobby) {
        return hobbyZoneRepository.findAllByHobbyId(hobby.getId()).stream()
                .map(HobbyZone::getZone)
                .toList();
    }

    public void addManager(Hobby hobby, Account account) {
        if (hobbyManagerRepository.existsByHobbyAndAccount(hobby, account)) {
            return;
        }
        hobbyManagerRepository.save(HobbyManager.builder().hobby(hobby).account(account).build());
        hobby.incrementMemberCount();
    }

    public void addMember(Hobby hobby, Account account) {
        if (hobbyMemberRepository.existsByHobbyAndAccount(hobby, account)) {
            return;
        }
        hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(account).build());
        hobby.incrementMemberCount();
    }

    public void removeMember(Hobby hobby, Account account) {
        hobbyMemberRepository.deleteByHobbyAndAccount(hobby, account);
        hobby.decrementMemberCount();
    }

    public boolean isManager(Hobby hobby, Account account) {
        return hobbyManagerRepository.existsByHobbyAndAccount(hobby, account);
    }

    public boolean isMember(Hobby hobby, Account account) {
        return hobbyMemberRepository.existsByHobbyAndAccount(hobby, account);
    }

    public boolean isJoinable(Hobby hobby, Account account) {
        return hobby.isPublished() && hobby.isRecruiting()
                && !isMember(hobby, account) && !isManager(hobby, account);
    }

    public List<Account> getManagers(Hobby hobby) {
        return hobbyManagerRepository.findAllByHobbyId(hobby.getId()).stream()
                .map(HobbyManager::getAccount)
                .toList();
    }

    public List<Account> getMembers(Hobby hobby) {
        return hobbyMemberRepository.findAllByHobbyId(hobby.getId()).stream()
                .map(HobbyMember::getAccount)
                .toList();
    }

    private void checkIfExistingHobby(String path, Hobby hobby) {
        if (hobby == null) {
            throw new BusinessException(ErrorCode.HOBBY_NOT_FOUND);
        }
    }

    private void checkIfManager(Account account, Hobby hobby) {
        if (!isManager(hobby, account)) {
            throw new AccessDeniedException("You do not have permission to access this hobby.");
        }
    }

    public void updateHobbyPath(Hobby hobby, String newPath) {
        if (!newPath.matches(VALID_PATH_PATTERN)) {
            throw new BusinessException(ErrorCode.INVALID_HOBBY_PATH);
        }
        if (hobbyRepository.existsByPath(newPath)) {
            throw new BusinessException(ErrorCode.HOBBY_PATH_ALREADY_EXISTS);
        }
        hobby.updatePath(newPath);
    }

    public void updateHobbyTitle(Hobby hobby, String newTitle) {
        if (hobbyRepository.existsByTitle(newTitle)) {
            throw new BusinessException(ErrorCode.HOBBY_TITLE_ALREADY_EXISTS);
        }
        hobby.updateTitle(newTitle);
    }

    public void publish(Hobby hobby) {
        hobby.publish();
        eventPublisher.publishEvent(new HobbyCreatedEvent(hobby));
    }

    public void close(Hobby hobby) {
        hobby.close();
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby closed"));
    }

    public void startRecruit(Hobby hobby) {
        hobby.startRecruit();
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby recruitment started"));
    }

    public void stopRecruit(Hobby hobby) {
        hobby.stopRecruit();
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby recruitment stopped"));
    }

    public void remove(Hobby hobby) {
        if(hobby.isRemovable()) {
            hobby.softDelete();
        } else {
            throw new BusinessException(ErrorCode.HOBBY_NOT_REMOVABLE);
        }
    }

}
