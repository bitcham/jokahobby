package com.jokahobby.modules.hobby;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.jokahobby.modules.hobby.Hobby.VALID_PATH_PATTERN;

@Service
@RequiredArgsConstructor
public class HobbyService {

    private final HobbyRepository hobbyRepository;
    private final HobbyTagRepository hobbyTagRepository;
    private final HobbyZoneRepository hobbyZoneRepository;
    private final HobbyHostRepository hobbyHostRepository;
    private final HobbyManagerRepository hobbyManagerRepository;
    private final HobbyMemberRepository hobbyMemberRepository;

    public Hobby createNewHobby(Hobby hobby, Account account) {
        if (hobbyRepository.existsByPath(hobby.getPath())) {
            throw new BusinessException(ErrorCode.HOBBY_PATH_ALREADY_EXISTS);
        }
        if (hobbyRepository.existsByTitle(hobby.getTitle())) {
            throw new BusinessException(ErrorCode.HOBBY_TITLE_ALREADY_EXISTS);
        }
        Hobby saved = hobbyRepository.save(hobby);
        addHost(saved, account);
        return saved;
    }

    public Page<Hobby> findPublished(String country, String city, HobbySortType sort, Pageable pageable) {
        return hobbyRepository.findPublished(country, city, sort, pageable);
    }

    public Page<Hobby> findByKeyword(String keyword, Pageable pageable) {
        return hobbyRepository.findByKeyword(keyword, pageable);
    }

    public Hobby getHobby(String path) {
        return hobbyRepository.findByPath(path)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOBBY_NOT_FOUND));
    }

    public Hobby getHobbyForUpdate(String path) {
        return hobbyRepository.findByPathForUpdate(path)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOBBY_NOT_FOUND));
    }

    public Hobby getHobbyWithHostCheckForUpdate(Account account, String path) {
        Hobby hobby = this.getHobbyForUpdate(path);
        checkIfHost(account, hobby);
        return hobby;
    }

    private void checkIfHost(Account account, Hobby hobby) {
        if (!isHost(hobby, account)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    public Hobby getHobbyWithHostOrManagerCheck(Account account, String path) {
        Hobby hobby = this.getHobby(path);
        if (!isHostOrManager(hobby, account)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return hobby;
    }

    public void updateHobbyDescription(Hobby hobby, String shortDescription, String fullDescription) {
        hobby.updateDescription(shortDescription, fullDescription);
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

    public Map<Long, List<Tag>> getTagsByHobbyIds(List<Long> hobbyIds) {
        if (hobbyIds.isEmpty()) {
            return Map.of();
        }
        return hobbyTagRepository.findAllByHobbyIdIn(hobbyIds).stream()
                .collect(Collectors.groupingBy(
                        ht -> ht.getHobby().getId(),
                        Collectors.mapping(HobbyTag::getTag, Collectors.toList())
                ));
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

    public void addHost(Hobby hobby, Account account) {
        hobbyHostRepository.save(HobbyHost.builder().hobby(hobby).account(account).build());
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

    public void removeManager(Hobby hobby, Account account) {
        hobbyManagerRepository.deleteByHobbyAndAccount(hobby, account);
        hobby.decrementMemberCount();
    }

    public boolean isHost(Hobby hobby, Account account) {
        return hobbyHostRepository.existsByHobbyAndAccount(hobby, account);
    }

    public boolean isManager(Hobby hobby, Account account) {
        return hobbyManagerRepository.existsByHobbyAndAccount(hobby, account);
    }

    public boolean isHostOrManager(Hobby hobby, Account account) {
        return isHost(hobby, account) || isManager(hobby, account);
    }

    public boolean isMember(Hobby hobby, Account account) {
        return hobbyMemberRepository.existsByHobbyAndAccount(hobby, account);
    }

    public boolean isJoinable(Hobby hobby, Account account) {
        return hobby.isPublished() && hobby.isRecruiting()
                && !isMember(hobby, account) && !isManager(hobby, account) && !isHost(hobby, account);
    }

    public Account getHost(Hobby hobby) {
        return hobbyHostRepository.findByHobbyId(hobby.getId())
                .map(HobbyHost::getAccount)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOBBY_HOST_NOT_FOUND));
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

    public void promoteToManager(Hobby hobby, Account target, Account promotedBy) {
        if (isHost(hobby, target)) {
            throw new BusinessException(ErrorCode.HOBBY_CANNOT_PROMOTE_HOST);
        }
        if (isManager(hobby, target)) {
            throw new BusinessException(ErrorCode.HOBBY_ALREADY_MANAGER);
        }
        if (!isMember(hobby, target)) {
            throw new BusinessException(ErrorCode.HOBBY_TARGET_NOT_MEMBER);
        }
        hobbyMemberRepository.deleteByHobbyAndAccount(hobby, target);
        hobbyManagerRepository.save(HobbyManager.builder()
                .hobby(hobby).account(target).promotedBy(promotedBy).build());
    }

    public void demoteToMember(Hobby hobby, Account target) {
        if (!isManager(hobby, target)) {
            throw new BusinessException(ErrorCode.HOBBY_TARGET_NOT_MANAGER);
        }
        hobbyManagerRepository.deleteByHobbyAndAccount(hobby, target);
        hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(target).build());
    }

    public void transferHost(Hobby hobby, Account newHost) {
        HobbyHost hobbyHost = hobbyHostRepository.findByHobbyId(hobby.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.HOBBY_HOST_NOT_FOUND));
        Account oldHost = hobbyHost.getAccount();

        if (oldHost.getId().equals(newHost.getId())) {
            throw new BusinessException(ErrorCode.HOBBY_TRANSFER_TARGET_INVALID);
        }

        if (isMember(hobby, newHost)) {
            hobbyMemberRepository.deleteByHobbyAndAccount(hobby, newHost);
        } else if (isManager(hobby, newHost)) {
            hobbyManagerRepository.deleteByHobbyAndAccount(hobby, newHost);
        } else {
            throw new BusinessException(ErrorCode.HOBBY_TRANSFER_TARGET_INVALID);
        }

        hobbyManagerRepository.save(HobbyManager.builder()
                .hobby(hobby).account(oldHost).promotedBy(newHost).build());

        hobbyHost.transferTo(newHost, oldHost);
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
    }

    public void close(Hobby hobby) {
        hobby.close();
    }

    public void startRecruit(Hobby hobby) {
        hobby.startRecruit();
    }

    public void stopRecruit(Hobby hobby) {
        hobby.stopRecruit();
    }

    public void remove(Hobby hobby) {
        if(hobby.isRemovable()) {
            hobby.softDelete();
        } else {
            throw new BusinessException(ErrorCode.HOBBY_NOT_REMOVABLE);
        }
    }

}
