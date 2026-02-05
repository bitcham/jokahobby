package com.jokahobby.modules.hobby;

import com.jokahobby.infra.exception.BusinessException;
import com.jokahobby.infra.exception.ErrorCode;
import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.hobby.event.HobbyCreatedEvent;
import com.jokahobby.modules.hobby.event.HobbyUpdateEvent;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import com.jokahobby.modules.hobby.form.HobbyDescriptionForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.jokahobby.modules.hobby.form.HobbyForm.*;

@Service
@RequiredArgsConstructor
public class HobbyService {

    private final HobbyRepository hobbyRepository;
    private final HobbyTagRepository hobbyTagRepository;
    private final HobbyZoneRepository hobbyZoneRepository;
    private final HobbyManagerRepository hobbyManagerRepository;
    private final HobbyMemberRepository hobbyMemberRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Hobby createNewHobby(Hobby hobby, Account account) {
        Hobby saved = hobbyRepository.save(hobby);
        addManager(saved, account);
        return saved;
    }

    public Hobby getHobby(String path) {
        Hobby hobby = this.hobbyRepository.findByPath(path);
        checkIfExistingHobby(path, hobby);
        return hobby;
    }

    public Hobby getHobbyToUpdate(Account account, String path) {
        Hobby hobby = this.getHobby(path);
        checkIfManager(account, hobby);
        return hobby;
    }

    @Transactional
    public void updateHobbyDescription(Hobby hobby, @Valid HobbyDescriptionForm hobbyDescriptionForm) {
       modelMapper.map(hobbyDescriptionForm, hobby);
       eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby description updated"));
    }

    @Transactional
    public void updateHobbyImage(Hobby hobby, String image) {
        hobby.setImage(image);
    }

    @Transactional
    public void enableHobbyBanner(Hobby hobby) {
        hobby.setUseBanner(true);
    }

    @Transactional
    public void disableHobbyBanner(Hobby hobby) {
        hobby.setUseBanner(false);
    }

    @Transactional
    public void addTag(Hobby hobby, Tag tag) {
        if (hobbyTagRepository.existsByHobbyAndTag(hobby, tag)) {
            return;
        }
        hobbyTagRepository.save(HobbyTag.builder().hobby(hobby).tag(tag).build());
    }

    @Transactional
    public void removeTag(Hobby hobby, Tag tag) {
        hobbyTagRepository.deleteByHobbyAndTag(hobby, tag);
    }

    public List<Tag> getTags(Hobby hobby) {
        return hobbyTagRepository.findAllByHobbyId(hobby.getId()).stream()
                .map(HobbyTag::getTag)
                .toList();
    }

    @Transactional
    public void addZone(Hobby hobby, Zone zone) {
        if (hobbyZoneRepository.existsByHobbyAndZone(hobby, zone)) {
            return;
        }
        hobbyZoneRepository.save(HobbyZone.builder().hobby(hobby).zone(zone).build());
    }

    @Transactional
    public void removeZone(Hobby hobby, Zone zone) {
        hobbyZoneRepository.deleteByHobbyAndZone(hobby, zone);
    }

    public List<Zone> getZones(Hobby hobby) {
        return hobbyZoneRepository.findAllByHobbyId(hobby.getId()).stream()
                .map(HobbyZone::getZone)
                .toList();
    }

    @Transactional
    public void addManager(Hobby hobby, Account account) {
        if (hobbyManagerRepository.existsByHobbyAndAccount(hobby, account)) {
            return;
        }
        hobbyManagerRepository.save(HobbyManager.builder().hobby(hobby).account(account).build());
        hobby.incrementMemberCount();
    }

    @Transactional
    public void addMember(Hobby hobby, Account account) {
        if (hobbyMemberRepository.existsByHobbyAndAccount(hobby, account)) {
            return;
        }
        hobbyMemberRepository.save(HobbyMember.builder().hobby(hobby).account(account).build());
        hobby.incrementMemberCount();
    }

    @Transactional
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

    public Hobby getHobbyToUpdateStatus(Account account, String path) {
        Hobby hobby = hobbyRepository.findByPath(path);
        checkIfExistingHobby(path, hobby);
        checkIfManager(account, hobby);
        return hobby;
    }

    public boolean isValidPath(String newPath) {
        if (!newPath.matches(VALID_PATH_PATTERN)){
            return false;
        }
        return !hobbyRepository.existsByPath(newPath);
    }

    @Transactional
    public void updateHobbyPath(Hobby hobby, String newPath) {
        hobby.setPath(newPath);
    }

    public boolean isValidTitle(String newTitle) {
         return newTitle.length() <= 50;
    }

    public boolean isDuplicatedTitle(String newTitle) {
        return hobbyRepository.existsByTitle(newTitle);
    }

    @Transactional
    public void updateHobbyTitle(Hobby hobby, String newTitle) {
        hobby.setTitle(newTitle);
    }

    @Transactional
    public void publish(Hobby hobby) {
        hobby.publish();
        eventPublisher.publishEvent(new HobbyCreatedEvent(hobby));
    }

    @Transactional
    public void close(Hobby hobby) {
        hobby.close();
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby closed"));
    }

    @Transactional
    public void startRecruit(Hobby hobby) {
        hobby.startRecruit();
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby recruitment started"));
    }

    @Transactional
    public void stopRecruit(Hobby hobby) {
        hobby.stopRecruit();
        eventPublisher.publishEvent(new HobbyUpdateEvent(hobby, "Hobby recruitment stopped"));
    }

    @Transactional
    public void remove(Hobby hobby) {
        if(hobby.isRemovable()) {
            hobbyRepository.delete(hobby);
        } else {
            throw new BusinessException(ErrorCode.HOBBY_NOT_REMOVABLE);
        }
    }

    public Hobby getHobbyToEnroll(String path) {
        Hobby hobby = hobbyRepository.findHobbyOnlyByPath(path);
        checkIfExistingHobby(path, hobby);
        return hobby;
    }
}
