package com.jokahobby.modules.hobby;

import com.jokahobby.modules.account.Account;
import com.jokahobby.modules.tag.Tag;
import com.jokahobby.modules.zone.Zone;
import com.jokahobby.modules.hobby.form.HobbyDescriptionForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.jokahobby.modules.hobby.form.HobbyForm.*;

@Service
@RequiredArgsConstructor
@Transactional
public class HobbyService {

    private final HobbyRepository hobbyRepository;
    private final ModelMapper modelMapper;

    public Hobby createNewHobby(Hobby hobby, Account account) {
        Hobby newHobby = hobbyRepository.save(hobby);
        newHobby.addManager(account);
        return newHobby;
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

    public void updateHobbyDescription(Hobby hobby, @Valid HobbyDescriptionForm hobbyDescriptionForm) {
       modelMapper.map(hobbyDescriptionForm, hobby);
    }

    public void updateHobbyImage(Hobby hobby, String image) {
        hobby.setImage(image);
    }

    public void enableHobbyBanner(Hobby hobby) {
        hobby.setUseBanner(true);
    }

    public void disableHobbyBanner(Hobby hobby) {
        hobby.setUseBanner(false);
    }

    public void addTag(Hobby hobby, Tag tag) {
        hobby.getTags().add(tag);
    }

    public void removeTag(Hobby hobby, Tag tag) {
        hobby.getTags().remove(tag);
    }

    public void addZone(Hobby hobby, Zone zone) {
        hobby.getZones().add(zone);
    }

    public void removeZone(Hobby hobby, Zone zone) {
        hobby.getZones().remove(zone);
    }

    public Hobby getHobbyToUpdateTag(Account account, String path) {
        Hobby hobby = hobbyRepository.findAccountWithTagsByPath(path);
        checkIfExistingHobby(path, hobby);
        checkIfManager(account, hobby);
        return hobby;
    }

    public Hobby getHobbyToUpdateZone(Account account, String path) {
        Hobby hobby = hobbyRepository.findAccountWithZonesByPath(path);
        checkIfExistingHobby(path, hobby);
        checkIfManager(account, hobby);
        return hobby;
    }

    private void checkIfExistingHobby(String path, Hobby hobby) {
        if (hobby == null) {
            throw new IllegalArgumentException("Hobby with path " + path + " does not exist.");
        }
    }

    private void checkIfManager(Account account, Hobby hobby) {
        if (!hobby.isManagedBy(account)) {
            throw new AccessDeniedException("You do not have permission to access this hobby.");
        }
    }

    public Hobby getHobbyToUpdateStatus(Account account, String path) {
        Hobby hobby = hobbyRepository.findHobbyWithManagersByPath(path);
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

    public void updateHobbyPath(Hobby hobby, String newPath) {
        hobby.setPath(newPath);
    }

    public boolean isValidTitle(String newTitle) {
         return newTitle.length() <= 50;
    }

    public boolean isDuplicatedTitle(String newTitle) {
        return hobbyRepository.existsByTitle(newTitle);
    }

    public void updateHobbyTitle(Hobby hobby, String newTitle) {
        hobby.setTitle(newTitle);
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

    public void remove(Hobby hobby) {
        if(hobby.isRemovable()) {
            hobbyRepository.delete(hobby);
        } else {
            throw new IllegalArgumentException("Hobby cannot be removed.");
        }
    }

    public void addMember(Hobby hobby, Account account) {
        hobby.addMember(account);
    }

    public void removeMember(Hobby hobby, Account account) {
        hobby.removeMember(account);
    }

    public Hobby getHobbyToEnroll(String path) {
        Hobby hobby = hobbyRepository.findHobbyOnlyByPath(path);
        checkIfExistingHobby(path, hobby);
        return hobby;
    }
}
